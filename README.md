![Concept](/img/diagram.jpg "Concept")

# Dismiss monorepo and use individual gitops repo for each app

1. Add gitops_repository parameter in workload.
2. The default branch for acceptance deployment is "act"
3. Remove parameters related to monorepo from from supplychain installation
   #### --gitops_repository_name 
   #### --gitops_server_address 
   #### --gitops_repository_owner 

# Create an acceptance clusterdelivery 
```
    kubeclt apply -f tap/clusterdelivery-sit.yaml
```
# Create a deliverable in acceptance namespace 
```
apiVersion: carto.run/v1alpha1
kind: Deliverable
metadata:
  name: fcb-web
  namespace: acctest
  labels:
    app.tanzu.vmware.com/deliverable-type: web
    app.tanzu.vmware.com/deliverable-target: acceptance
spec:
  serviceAccountName: default
  params:
    - name: "gitops_ssh_secret"
      value: git-ssh-secret-1
  source:
    git:
      url: ssh://gitlab.h2o-4-2180.h2o.vmware.com/Jeffrey/fcb-web-gitops.git
      ref:
        branch: act
    subPath: config
```


# Pass workload parameters to deliverable
## Modify deliverable-template
kubectl edit ClusterTemplate deliverable-template

```
apiVersion: carto.run/v1alpha1
kind: Deliverable
metadata:
name: #@ data.values.workload.metadata.name
...

      #@ if/end is_gitops():
      params:
        - name: "gitops_ssh_secret"
          value: #@ param("gitops_ssh_secret")
        #@ if hasattr(data.values.workload.spec, "params"):
        #@ for i in range(len(data.values.workload.spec.params)):
        - name: #@ data.values.workload.spec.params[i].name
          value: #@ data.values.workload.spec.params[i].value
        #@ end
        #@ end
```

# Add an integration operation after successful deployment

kubectl edit ClusterDelivery delivery-basic
````
  - deployment:
      resource: source-provider
    name: deployer
    params:
    - name: serviceAccount
      value: default
    templateRef:
      kind: ClusterDeploymentTemplate
      name: app-deploy
  - name: integration-testing
    deployment:
      resource: deployer
    templateRef:
      kind: ClusterDeploymentTemplate
      name: integration-test
  - name: sit-deployment
    deployment:
      resource: integration-testing
    params:
    - name: serviceAccount
      value: default
    templateRef:
      kind: ClusterDeploymentTemplate
      name: deploy-sit
````
# Customize Cluster Delivery
```
   kubectl apply -f tap/tasktemp.yaml
   kubectl apply -f tap/sittemp.yaml
   kubectl apply -f tap/delivery.yaml
   kubectl apply -f tap/deliverable-template.yaml
```
# If use web workload, enable tls cert delegation (Modify it before apply)
```
  kubectl apply -f tap/secret-delegate.yaml
```

# Testing workload
``````
tanzu apps workload update tanzu-java-web-app \
--app tanzu-java-web-app \
--git-repo ssh://gitlab.h2o-4-2180.h2o.vmware.com/Jeffrey/application-accelerator-samples.git \
--git-branch main \
--type web \
--label app.kubernetes.io/part-of=tanzu-java-web-app \
--yes \
--namespace workloads \
--param "actest-namespace=acctest" 
--sub-path tanzu-java-web-app \
--param "gitops_repository=" ssh://gitlab.h2o-4-2180.h2o.vmware.com/Jeffrey/fcb-web.git \
--param "git_int_testing_repo=ssh://gitlab.h2o-4-2180.h2o.vmware.com/Jeffrey/tanzu-java-web-app.git" \
--param "git_int_testing_rev=main" \
--param "git_int_testing_url=http://tanzu-java-web-app.workloads.apps.tap.h2o-4-2180.h2o.vmware.com/" \
--param-yaml integration_testing_matching_labels='{"apps.tanzu.vmware.com/pipeline":"int-test"}'

``````







