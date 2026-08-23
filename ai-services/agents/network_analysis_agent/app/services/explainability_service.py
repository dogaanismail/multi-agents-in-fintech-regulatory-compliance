"""
TreeSHAP explainability for the network analysis agent.

Uses CatBoost's native TreeSHAP (get_feature_importance type=ShapValues) rather
than the shap package: identical algorithm, no model-format parsing that breaks
across library versions. The model scores a scaled copy of a named feature
frame, so contributions map one-to-one onto the input features; the raw
(unscaled) value is reported so officers read "pagerank = 0.021", not a z-score.

Explanation failures never fail the prediction: the decision proceeds and the
contributions are simply absent.
"""

import numpy as np
from catboost import Pool

from ..core.logging import logger


class ExplainabilityService:

    def __init__(self):
        self._model = None

    def initialise(self, model) -> None:
        try:
            self._model = model
            logger.info("✅ Native TreeSHAP ready")
        except Exception as exc:
            self._model = None
            logger.warning(f"TreeSHAP initialisation failed: {exc}")

    @property
    def is_ready(self) -> bool:
        return self._model is not None

    def explain(self, features_scaled, features_df, top_n: int = 24):
        """Returns (base_value, contributions) or (None, []) on any failure."""
        if not self.is_ready:
            return None, []
        try:
            row = np.asarray(features_scaled)[:1]
            shap_values = self._model.get_feature_importance(
                type="ShapValues", data=Pool(row)
            )
            shap_row = np.asarray(shap_values)[0, :-1]
            base_value = float(np.asarray(shap_values)[0, -1])

            contributions = self._to_contributions(shap_row, features_df)
            contributions.sort(key=lambda c: abs(c["shap_value"]), reverse=True)
            return base_value, contributions[:top_n]
        except Exception as exc:
            logger.warning(f"TreeSHAP explanation failed: {exc}")
            return None, []

    def _to_contributions(self, shap_row, features_df):
        contributions = []
        for index, feature in enumerate([str(c) for c in features_df.columns]):
            shap_value = float(shap_row[index])
            contributions.append({
                "feature": feature,
                "value": self._display_value(features_df, feature),
                "shap_value": shap_value,
                "direction": "INCREASES_RISK" if shap_value > 0
                else "DECREASES_RISK" if shap_value < 0
                else "NO_IMPACT",
            })
        return contributions

    @staticmethod
    def _display_value(features_df, feature: str) -> str:
        raw = features_df[feature].iloc[0]
        if isinstance(raw, float):
            return f"{raw:.4g}"
        return str(raw)


explainability_service = ExplainabilityService()
