"""
TreeSHAP explainability for the transaction pattern agent.

Uses XGBoost's native TreeSHAP (predict with pred_contribs=True) rather than the
shap package: identical algorithm, no model-format parsing that breaks across
xgboost versions. The model scores a one-hot encoded vector, so contributions of
encoded columns are summed back to the original input feature — officers see
"Payment_type = Cross-border" with one signed value, not 57 columns.

Explanation failures never fail the prediction: the decision proceeds and the
contributions are simply absent.
"""

import numpy as np
import xgboost as xgb

from ..core.logging import logger


class ExplainabilityService:

    def __init__(self):
        self._booster = None
        self._transformed_feature_names = None

    def initialise(self, model, preprocessor) -> None:
        try:
            self._booster = model.get_booster()
            self._transformed_feature_names = [
                str(name) for name in preprocessor.get_feature_names_out()
            ]
            logger.info(
                f"✅ Native TreeSHAP ready "
                f"({len(self._transformed_feature_names)} encoded features)"
            )
        except Exception as exc:
            self._booster = None
            logger.warning(f"TreeSHAP initialisation failed: {exc}")

    @property
    def is_ready(self) -> bool:
        return self._booster is not None

    def explain(self, x_transformed, input_df, top_n: int = 24):
        """Returns (base_value, contributions) or (None, []) on any failure."""
        if not self.is_ready:
            return None, []
        try:
            contribs = self._booster.predict(
                xgb.DMatrix(self._densify_single_row(x_transformed)), pred_contribs=True
            )
            shap_row = np.asarray(contribs)[0, :-1]
            base_value = float(np.asarray(contribs)[0, -1])

            grouped = self._group_by_original_feature(shap_row, input_df)
            grouped.sort(key=lambda c: abs(c["shap_value"]), reverse=True)
            return base_value, grouped[:top_n]
        except Exception as exc:
            logger.warning(f"TreeSHAP explanation failed: {exc}")
            return None, []

    def _group_by_original_feature(self, shap_row, input_df):
        original_features = [str(c) for c in input_df.columns]
        totals = {}
        for index, encoded_name in enumerate(self._transformed_feature_names):
            feature = self._original_feature_for(encoded_name, original_features)
            totals.setdefault(feature, 0.0)
            totals[feature] += float(shap_row[index])

        contributions = []
        for feature, shap_value in totals.items():
            contributions.append({
                "feature": feature,
                "value": self._display_value(input_df, feature),
                "shap_value": shap_value,
                "direction": "INCREASES_RISK" if shap_value > 0
                else "DECREASES_RISK" if shap_value < 0
                else "NO_IMPACT",
            })
        return contributions

    @staticmethod
    def _original_feature_for(encoded_name: str, original_features) -> str:
        bare = encoded_name.split("__", 1)[-1]
        for feature in original_features:
            if bare == feature or bare.startswith(f"{feature}_"):
                return feature
        return bare

    @staticmethod
    def _display_value(input_df, feature: str) -> str:
        if feature in input_df.columns:
            return str(input_df[feature].iloc[0])
        return ""

    @staticmethod
    def _densify_single_row(x_transformed):
        if hasattr(x_transformed, "toarray"):
            return x_transformed[:1].toarray()
        return np.asarray(x_transformed)[:1]


explainability_service = ExplainabilityService()
