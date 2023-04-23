# 概述
本方案采用multi-repo的设计而不是TAP默认的mono-repo配置。即：为每一个project创建一个{project-name}-gitops的repo,  
每个gitops可以根据实际部署的需求创建多个branch，如：dev,sit,hotfix,prod等。不同部署环境的kapp只需watch对应的branch  
即可实现在本环境下app的动态更新。  

![Concept](/img/diagram.jpg "Concept")  

这样的设计有如下benefit：  
1. 可以避免在有很多project时，任何一个project更新触发的所有project的gitrepository并发访问repo对git server造成的压力。  
2. 可以避免在有很多project时，任何一个project更新触发有sit testing的supplychain的运行。  
3. 基于多branch的设计可以优雅的实现多环境的部署。  
   

# 为每一个workload创建一个gitops的repo并添加到worload的manifest  
1. 为workload增加 gitops_repository 参数.  
2. 在gitops repo中，默认集成测试的branch为 "act".    
3. 移除安装supplychain时和mono-repo相关的配置：“server_address”，“repository_owner”，“repository_name”.  
   #### --gitops_repository_name   
   #### --gitops_server_address  
   #### --gitops_repository_owner   


# 在active namespace为default用户创建clusterrolebinding  
在sit testing的tekton pipeline中使用了clustertask的CR,default需要能访问clustertask api的权限。
kubectl create clusterrolebinding fcb-cluster-pipeline \
    --clusterrole=tekton-pipelines-app-operator-cluster-access \
    --serviceaccount=[ns]:default

# 创建独立的acceptance clusterdelivery  
在acceptance namespace中部署sit测试后的的app使用独立的delivery.   
delivey使用selector：  
  app.tanzu.vmware.com/deliverable-target: acceptance  
来选择部署到acceptance namespce的app。  

```
    kubeclt apply -f tap/clusterdelivery-sit.yaml
```

# 在acceptance namespace中创建deliverable  
我们需要在acceptance namespace中创建好deliverable, 监视gitops repo中act branch的更新来更新app。

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

# 传递workload参数到deliverable  
我们在定制delivey中定制部署后的sit testing时，需要用到workload中设定的参数。如测试script的repo。  
由于supplychain（workload）和delivey使用的是各自独立的value空间，所以需要通过模版实现参数从workload  
到deliverable的传递。

## 修改deliverable-template
kubectl edit ClusterTemplate deliverable-template

```yaml
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

# 扩展默认的delivery使其支持sit testing并在测试成功后部署到acceptance namespace

kubectl edit ClusterDelivery delivery-basic
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
```
# 部署客制化的Cluster Delivery
```
   kubectl apply -f tap/tasktemp.yaml
   kubectl apply -f tap/sittemp.yaml
   kubectl apply -f tap/delivery.yaml
   kubectl apply -f tap/deliverable-template.yaml
```
# 如果使用的是web workload, enable tls cert delegation (apply前根据实际配置修改yaml文件)
```
  kubectl apply -f tap/secret-delegate.yaml
```

# Workload 参数说明
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
--param "gitops_repository=" ssh://gitlab.h2o-4-2180.h2o.vmware.com/Jeffrey/fcb-web-gitopos.git \
--param "git_int_testing_repo=ssh://gitlab.h2o-4-2180.h2o.vmware.com/Jeffrey/tanzu-java-web-app.git" \
--param "git_int_testing_rev=main" \
--param "git_int_testing_url=http://tanzu-java-web-app.workloads.apps.tap.h2o-4-2180.h2o.vmware.com/" \
--param-yaml integration_testing_matching_labels='{"apps.tanzu.vmware.com/pipeline":"int-test"}'

``````

*actest-namespace：acceptance namespae，集成测试后的app要部署到此namespace  
*gitops_repository：gitops repo的设置  
*git_int_testing_repo：集成测试script的repo.  
*git_int_testing_rev: 集成测试repo的revision.  
*git_int_testing_url: 集成测试目标app部署完成后生成的url, 如果是knative app，系统会自动生成ingress url。如果是  
&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;service workload，可以通过客制化supplychain生成ingress url。也可以使用部署完成后的clusterip service.  
*integration_testing_matching_labels: 此应用是否使用集成测试delivery. 如果不设定，不会启动集成测试。                     







