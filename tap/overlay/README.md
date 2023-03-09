# Overlay ootb-template to add below features for server workload
  ### 1. PV/pvc/secret vol/configmap vol
  ### 2. Health probe (for non-spring-boot apps)
  ### 3. Dynamic pod and service port
  ### 4. Ingress
  ### 5. Pass workload parameters to delivery


# Deploy the overly

## Create the secret
```
    kubectl apply -f ootb-template-add-pvc-probe-ingress-overlay.yaml
    kubectl apply -f ootb-template-add-workload-param-overlay.yaml   
```
## Apply the overlay

### Installation with profile
```
   ## Modify tap-values
   package_overlays:
   - name: ootb-templates
     secrets:
     - name: ootb-templates-convention-tempalte-overlay
     - name: ootb-templates-parameter-overlay

   ## Update the package
   tanzu package installed update ootb-templates -v {version} -f tap-values.yaml -n tap-install
```

### Installation component by component
```
   kubectl annotate pkgi ootb-templates ext.packaging.carvel.dev/ytt-paths-from-secret-name.0=ootb-templates-convention-tempalte-overlay -n tap-install
   kubectl annotate pkgi ootb-templates ext.packaging.carvel.dev/ytt-paths-from-secret-name.1=ootb-templates-parameter-overlay -n tap-install
```

## Workload format
```
tanzu apps workload create fcb-python \
--git-repo https://github.com/JeffreyWa/tap-sit-pipeline.git \
--git-branch main \
--sub-path tanzu-python-web-app \
--type server \
--env FLASK_RUN_HOST=0.0.0.0 \
--env FLASK_RUN_PORT=8082 \
--label app.kubernetes.io/part-of=fcb-python \
--yes \
--namespace workloads \
--param "gitops_sub_path=config" \
--param-yaml ingress='{"apiVersion":"projectcontour.io/v1","kind":"HTTPProxy","metadata":{"name":"fcb-python","namespace":"workloads"},"spec":{"virtualhost":{"fqdn":"fcb-python.tap.h2o-4-2180.h2o.vmware.com","tls":{"secretName":"workloads/tap-ca-secret"}},"routes":[{"conditions":[{"prefix":null}],"services":[{"name":"fcb-python","port":8083}]}]}}' \
--param "gitops_repository=ssh://git@gitlab.h2o-4-2180.h2o.vmware.com/Jeffrey/fcb-python-gitops.git" \
--param-yaml volumes='[{"name":"tap-registry","secret":{"secretName":"tap-registry"}}]' \
--param-yaml volumeMounts='[{"name":"tap-registry","mountPath":"/tmp/tap-registry"}]' \
--param-yaml ports='[{"containerPort":8082,"port":8083,"name":"http"}]' \
--param-yaml livenessProbe='{"httpGet":{"path":"/livez","port":8082},"initialDelaySeconds":5,"timeoutSeconds":1}' \
--param-yaml readinessProbe='{"httpGet":{"path":"/readyz","port":8082},"initialDelaySeconds":5,"timeoutSeconds":1}'
```

### Add PV
   Add two workload parameters: volumes and volumeMounts. Just follow k8s standard format for them
   *** For knative workload, you should have to add another overlay
   ```
    #@ load("@ytt:data", "data")
    #@ load("@ytt:overlay", "overlay")

    #@overlay/match by=overlay.subset({"metadata":{"name":"config-features"}, "kind": "ConfigMap"})
    ---
    data:
    #@overlay/match missing_ok=True
    kubernetes.podspec-persistent-volume-claim: "enabled"
    #@overlay/match missing_ok=True
    kubernetes.podspec-persistent-volume-write: "enabled"
   ```
   And then
   ```
     kubectl annotate pkgi ootb-templates ext.packaging.carvel.dev/ytt-paths-from-secret-name.2=ootb-templates-knative-pv-overlay -n tap-install   
   ```
### Add dynamic ports for pod and service
   1. Add environment variables to workload and applicaiton applies env-var during starting.
      e.g. for spring boot apps 
      ```
        - env:
          - name: JAVA_TOOL_OPTIONS
            value: -Dserver.port="8088"
      ```
   2. Add ports paramaters to workload for service ports 

### Add health proble
    Follow [this link](https://docs.vmware.com/en/VMware-Tanzu-Application-Platform/1.4/tap/spring-boot-conventions-reference-CONVENTIONS.html). 

### Add ingress.
    You should bring your own ingress as a parameter for workload.
    This demo uses contour HTTPProxy as an exmaple
    ```
    apiVersion: projectcontour.io/v1
    kind: HTTPProxy
    metadata:
    name: fcb-python
    namespace: workloads
    spec:
    virtualhost:
        fqdn: fcb-python.tap.h2o-4-2180.h2o.vmware.com
        tls:
        secretName: workloads/tap-ca-secret
    routes:
        - conditions:
        - prefix:
        services:
            - name: fcb-python
            port: 8083
    ```