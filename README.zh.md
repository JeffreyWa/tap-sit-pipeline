# 概述
本方案採用 multi-repo 的設計而不是 TAP 默認的 mono-repo 配置。即：為每一個 project 創建一個 {project-name}-gitops 的 repo，
每個 GitOps 可以根據實際部署的需求創建多個 branch，例如：dev、sit、hotfix、prod 等。不同部署環境的 kapp 只需 watch 對應的 branch，
即可實現在本環境下應用的動態更新。 

![Concept](/img/diagram.jpg "Concept")  


這樣的設計有以下好處：

1. 當有很多項目時，可以避免任何一個項目的更新觸發對所有項目的 git 庫進行並發訪問，從而減少對 git 服務器的壓力。
2. 當有很多項目時，可以避免任何一個項目的更新觸發需要進行 SIT 測試的供應鏈運行。
3. 基於多分支的設計可以優雅地實現多環境的部署。 
   

# 為每個 Workload 創建一個 GitOps 的 repoistory，並將其添加到 Workload的manifest。 
1. 為 workload 增加 gitops_repository 參數.  
2. 在 gitops repo 中，默認整合測試的 branch 為 "act".    
3. 移除安裝 supplychain 時和 mono-repo 相關的配置：“server_address”，“repository_owner”，“repository_name”.  
   #### --gitops_repository_name   
   #### --gitops_server_address  
   #### --gitops_repository_owner   

# 創建獨立的acceptance clusterdelivery  
在 acceptance namespace 中部署 sit 測試後的 app 使用獨立的delivery.
delivey 使用 selector：  
  app.tanzu.vmware.com/deliverable-target: acceptance  
來選擇部署到 acceptance namespce 的 app。  
```
    kubectl apply -f tap/clusterdelivery-sit.yaml
```

# 在 acceptance namespace 中 創建 deliverable  
我們需要在 acceptance namespace 中創建好 deliverable, 監控 gitops repo 中 act branch 的更新來更新 app。

```yaml
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

# 傳遞 workload 参數到 deliverable  
我們在定制 delivery 中定制部署後的 SIT 測試時，需要使用 workload 中設定的參數，例如測試 script 的 repo。由於 supplychain（workload）和 delivery 使用各自獨立的 value 空間，因此需要透過 template 實現參數從 workload 到 deliverable 的傳遞。

# 使用 overlay

kubectl apply -f tap/overlay/ootb-templates-parameter-overlay.yml -n tap-install

修改 tap-values.yml
```
package_overlays:
  - name: ootb-templates
    secrets:
      - name: ootb-templates-git-writer-overlay
      - name: ...
      - name: ...
```
tanzu package installed update tap -f tap-values.yml -n tap-install

# 擴展預設的 delivery，使其支援 SIT 測試並在測試成功後部署到 acceptance namespace。

kubectl apply -f tap/clusterdelivery-act.yml
```yaml
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
  selector:
    app.tanzu.vmware.com/deliverable-type: web
    app.tanzu.vmware.com/act-tests: "true"
```
# 部署客制化的Cluster Delivery
```
   kubectl apply -f tap/tasktemp.yaml
   kubectl apply -f tap/sittemp.yaml
```
# Workload 参數說明

```yaml
apiVersion: carto.run/v1alpha1
kind: Workload
metadata:
  labels:
    app.kubernetes.io/part-of: tanzu-java-web-app
    apps.tanzu.vmware.com/act-tests: "true"
    apps.tanzu.vmware.com/has-tests: "true"
    apps.tanzu.vmware.com/workload-type: server
  name: tanzu-java-web-app
  namespace: tap-dev
spec:
  params:
  - name: actest-namespace
    value: tap-delivery
  - name: testing_pipeline_matching_labels
    value:
      apps.tanzu.vmware.com/pipeline: vmware
  - name: integration_testing_matching_labels
    value:
      apps.tanzu.vmware.com/pipeline: int-test
  source:
    git:
      ref:
        branch: dev
      url: https://github.com/timwangvmw/tanzu-java-web-app133.git
```
* apps.tanzu.vmware.com/act-tests: "true", 若沒有設定此 label, 則不會有整合測試.
* actest-namespace：acceptance namespae，整合測試後的 app 要部署到此namespace  

* testing_matching_labels: unit test, 預設為 testing。 

* integration_testing_matching_labels: integration test, 預設為 int-test。                     







