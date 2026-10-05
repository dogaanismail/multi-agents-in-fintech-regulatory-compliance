# Terraform (cloud demo environments)

One `terraform apply` creates a Kubernetes cluster and installs the same platform that runs locally on kind: the
operators, ArgoCD and the `bank-solution-apps` ApplicationSets. ArgoCD then syncs every store and service from a
**release tag**, running the images the **Publish Images** workflow pushed for that tag.

| Path                         | What it is                                                                                |
|------------------------------|-------------------------------------------------------------------------------------------|
| `modules/aws-cluster`        | VPC (2 AZs, one NAT gateway), EKS, EBS CSI driver, default encrypted `gp3` StorageClass   |
| `modules/platform-bootstrap` | Cloud-neutral: namespaces, generated credentials, priority classes, Strimzi, CNPG, ArgoCD |
| `environments/aws-demo`      | The deployable root: wires the two modules together                                       |

`platform-bootstrap` reads `infrastructure/kubernetes` (the files ArgoCD reads) to decide which credentials to create:
a PostgreSQL role per entry in `data/<namespace>/<store>.yaml`, a Kafka user per service with `messaging.enabled`, and
the Neo4j secret. Adding a service or database there needs no Terraform change.

## Releasing

Images are GitHub Packages on `ghcr.io/<owner>/bank-solution/<image>`, one package per entry in
`infrastructure/container-images.txt` (also read by `build-images.sh` for kind).

```bash
git tag v1.0.0 && git push origin v1.0.0       # Publish Images builds and pushes all 25 images
```

Each image is tagged `1.0.0` and `sha-<commit>`, and labelled with `org.opencontainers.image.source`, which links the
package to this repository. The workflow can also be started by hand (Actions → Publish Images); that run pushes only
`sha-<commit>` tags. After the first publish, check every package under the repository's **Packages** and set its
visibility to **public** if it is private, because the cluster pulls without credentials.

## Deploying a demo

Prerequisites: Terraform ≥ 1.9, AWS CLI v2 logged in to the target account, `kubectl`.

```bash
cd infrastructure/terraform/environments/aws-demo
cp terraform.tfvars.example terraform.tfvars    # set release, region, your IP in api_allowed_cidrs
terraform init
terraform apply                                 # ~20 min; ArgoCD needs ~10 more to sync everything
$(terraform output -raw configure_kubectl)
kubectl get applications -n argocd
```

The release must be a tag that already contains the `environments/aws-demo` values and whose images were published.
Generated passwords live only in the Terraform state (local, git-ignored) and in the cluster's Secrets.

Nothing is exposed publicly yet: reach the services the same way as on kind, with `kubectl port-forward`.

## Cost and teardown

Three `m6i.xlarge` nodes, the EKS control plane and one NAT gateway cost roughly **$0.80/hour**. Create the
environment before a demo and destroy it afterwards:

```bash
list_leftover_volumes=$(terraform output -raw leftover_volumes)
terraform destroy
eval "$list_leftover_volumes"                   # should print []
```

Volumes are created by Kubernetes, not Terraform, so a volume whose claim outlives the cluster is not deleted by
`destroy`. Every such volume is tagged `bank-solution/cluster=<name>`; delete any that the last command still lists.

## Keeping versions in step

The operator chart versions (Strimzi 1.2.0, CloudNativePG 0.29.1, Argo CD 10.9.2) appear in both
`modules/platform-bootstrap` and `infrastructure/kubernetes/helmfile.yaml.gotmpl`; bump them together.
