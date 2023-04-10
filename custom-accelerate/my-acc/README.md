![App template](/img/app-temp.jpg "App template")
![App probe](/img/acc-probe.jpg "App probe")
![App volume](/img/acc-vol.jpg "App volume")

# Create a custom accelerator
```
tanzu accelerator create my-tanzu-java-web-app \
  --git-repo https://gitlab.h2o-4-2180.h2o.vmware.com/Jeffrey/my-accelerator.git \
  --git-branch main \
  --interval 10s \
  --secret-ref git-credentials

```
# Create a custom accelerator fragment
```
tanzu accelerator fragment create custom-volume \
  --git-repo https://gitlab.h2o-4-2180.h2o.vmware.com/Jeffrey/my-accelerator.git \
  --git-branch main \
  --git-sub-path fragments/volume \
  --interval 10s \
  --secret-ref git-credentials

tanzu accelerator fragment create health-probe \
  --git-repo https://gitlab.h2o-4-2180.h2o.vmware.com/Jeffrey/my-accelerator.git \
  --git-branch main \
  --git-sub-path fragments/healthprobe \
  --interval 10s \
  --secret-ref git-credentials
```