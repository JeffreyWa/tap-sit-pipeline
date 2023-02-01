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
````

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
--sub-path tanzu-java-web-app \
--param "git_int_testing_repo=ssh://gitlab.h2o-4-2180.h2o.vmware.com/Jeffrey/tanzu-java-web-app.git" \
--param "git_int_testing_rev=main" \
--param "git_int_testing_url=http://tanzu-java-web-app.workloads.apps.tap.h2o-4-2180.h2o.vmware.com/" \
--param-yaml integration_testing_matching_labels='{"apps.tanzu.vmware.com/pipeline":"int-test"}'

``````







