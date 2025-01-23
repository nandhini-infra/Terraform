import com.tools.*
import com.pipelineinfra.*
import com.tools.checkout.*
import com.tools.deployment.*
import com.tools.authentication.*
import com.tools.build.*
import com.tools.scm.*
import com.tools.notification.*
import com.tools.helper.CredentialHelper
import groovy.json.*

def call() {
    def config
    def constants
    def pipelineOptions
    String FAILED_STAGE
    String ERROR_MESSAGE
    def defaultValuesFileString = libraryResource "default-values/values.yaml"
    def defaultValuesMap = readYaml(text: defaultValuesFileString)
    String podLabel = "gradle-slave-${UUID.randomUUID().toString()}"
    String podYaml = yamlBuilder.buildNowPipelineInfraPodYaml(podLabel, env, defaultValuesMap.constants.jenkinsIamRole)
    def configsRepo = defaultValuesMap.constants.configsRepo
    def configsRepoURL = "https://${defaultValuesMap.constants.gitHost}/${defaultValuesMap.constants.gitOrg}/${defaultValuesMap.constants.configsRepo}.git"
    def templatesRepo = defaultValuesMap.constants.templatesRepo
    def templatesRepoURL = "https://${defaultValuesMap.constants.gitHost}/${defaultValuesMap.constants.gitOrg}/${defaultValuesMap.constants.templatesRepo}.git"
    def west2ConfigsRepo = defaultValuesMap.constants.west2ConfigsRepo
    def west2ConfigsRepoURL = "https://${defaultValuesMap.constants.gitHost}/${defaultValuesMap.constants.gitOrg}/${defaultValuesMap.constants.west2ConfigsRepo}.git"
    def accountsObj
    def accObj
    def cidrObj
    def enviroment
    def vpc_cidr
    def private_subnet_cidr_1
    def private_subnet_cidr_2
    def private_subnet_cidr_3
    def accountsObjWest2
    def accObjWest2
    def cidrObjWest2
    def vpc_cidr_west2
    def private_subnet_cidr_1_west2
    def private_subnet_cidr_2_west2
    def private_subnet_cidr_3_west2
    def short_name
    def manager
    def cost_center
    def internal_order
    def app_id
    def app_owner	
    def business_group
    def AWS_REGION = "us-east-1"
    def AWS_RESOURCE_TYPE = params.get("AWS_RESOURCE_TYPE")
    def ACCOUNT_ID = params.get("ACCOUNT_ID")
    def APPLICATION_NAME = params.get("APPLICATION_NAME")
    def RESOURCE_NAME_SUFFIX = params.get("RESOURCE_NAME_SUFFIX")
    def ENVIRONMENT = params.get("ENVIRONMENT")
    def METALLIC_CLASSIFICATION = params.get("METALLIC_CLASSIFICATION")
    def ENGINE_VERSION = params.get("ENGINE_VERSION")
    def ENGINE_MODE = params.get("ENGINE_MODE")
    def PLATINUM_INSTANCE_TYPE = params.get ("PLATINUM_INSTANCE_TYPE")
    def NUM_CACHE_CLUSTER = params.get("NUM_CACHE_CLUSTER")
    def REDIS_NODE_TYPE = params.get("REDIS_NODE_TYPE")
    def REDIS_VERSION = params.get("REDIS_VERSION")
    def REDIS_FAILOVER = params.get("REDIS_FAILOVER")
    def MULTI_AZ_ENABLED = params.get("MULTI_AZ_ENABLED")
    def REDIS_FAMILY = params.get("REDIS_FAMILY")
    def AMI_ID  = params.get("AMI_ID")
    def INSTANCE_TYPE = params.get("INSTANCE_TYPE")
    def DELETE_BACKUP_AFTER_DAYS = params.get("DELETE_BACKUP_AFTER_DAYS")
    def BACKUP_SCHEDULE = params.get("BACKUP_SCHEDULE")
    def CONTINUOUS_BACKUP = params.get("CONTINUOUS_BACKUP")
    def NAME  = params.get("NAME")
    def DOMAIN_NAME = params.get("DOMAIN_NAME")
    def MAXIMUM_MESSAGE_SIZE       = params.get("MAXIMUM_MESSAGE_SIZE")
    def DELIVERY_DELAY              = params.get("DELIVERY_DELAY")
    def RECEIVE_WAIT_TIME_SECONDS  = params.get("RECEIVE_WAIT_TIME_SECONDS")
    def DEAD_LETTER_QUEUE_ARN      = params.get("DEAD_LETTER_QUEUE_ARN")
    def MESSAGE_RETENTION_SECONDS  = params.get("MESSAGE_RETENTION_SECONDS")
    def VISIBILITY_TIMEOUT_SECONDS = params.get("VISIBILITY_TIMEOUT_SECONDS")     
    def ORIGIN_BUCKET = params.get("ORIGIN_BUCKET")
    def ALB_NAME = params.get("ALB_NAME")
    def TARGET_GROUP_NAME = params.get("TARGET_GROUP_NAME")
    def ALLOWED_TARGET_GROUP_STATUS_CODE = params.get("ALLOWED_TARGET_GROUP_STATUS_CODE")
    def INGRESS_CONTROLLER_DNS = params.get("INGRESS_CONTROLLER_DNS")
    def SUBDOMAIN = params.get("SUBDOMAIN")
    def APP_HOSTED_ZONE = params.get("APP_HOSTED_ZONE")
    def create_application_urls = params.get("CREATE_APPLICATIONS_URLS")
    def SESNetworkAccountId = params.get("SESNetworkAccountId")	
    //def OPERATION = params.get("OPERATION")
    def EBS_VOLUME_TYPE = params.get("EBS_VOLUME_TYPE")
    def EBS_VOLUME_SIZE = params.get("EBS_VOLUME_SIZE")	

    //def step functions


    def subdomain
        if (SUBDOMAIN == ""){
            subdomain = "default"
        }
        else {
            subdomain = "$SUBDOMAIN"
        }
	
    def create_record	
    def app_hosted_zone
	if (APP_HOSTED_ZONE == ""){
                create_record = false
                app_hosted_zone = "$APP_HOSTED_ZONE"
            }
         else {
                create_record = true
                app_hosted_zone = "$APP_HOSTED_ZONE"
            }
	
	
    def short_domain
        switch (METALLIC_CLASSIFICATION) {
        case 'silver':
            short_domain = 's'
            break
        case 'gold':
            short_domain = 'g'
            break
        case 'gold-plus':
            short_domain = 'gp'
            break
        case 'platinum':
            short_domain = 'p'
        }
 
	
    def HASH_KEY = params.get("HASH_KEY")
    def FIFO_QUEUE = params.get("FIFO_QUEUE")
    def PARENT_ENVIRONMENT = "nonprod"
    if(ENVIRONMENT == "dev" || ENVIRONMENT == "qa") {
       PARENT_ENVIRONMENT = "nonprod"}
    if(ENVIRONMENT == "stage" || ENVIRONMENT == "prod") {
       PARENT_ENVIRONMENT = "prod"}
    def credentialID = defaultValuesMap.constants.gitCredentialID
    def infrastructureRepo = defaultValuesMap.constants.infrastructureRepo
    def infrastructureRepoURL = "https://${defaultValuesMap.constants.gitHost}/${defaultValuesMap.constants.gitOrg}/${defaultValuesMap.constants.infrastructureRepo}.git"
    def jobName = env.JOB_NAME
    def tString = jobName.replaceFirst("AccountInfrastructure/", "")
    def jobNameString = "${tString}".replaceAll('/', '-')
    def branchName = 'feature/'+APPLICATION_NAME+'-'+ENVIRONMENT+'-'+jobNameString+'-'+env.BUILD_NUMBER
    def folderName = APPLICATION_NAME+'/'+PARENT_ENVIRONMENT+'/'+ENVIRONMENT
    def parentFolder = APPLICATION_NAME+'/'+PARENT_ENVIRONMENT
    def creds
    def plan
    pipeline {
        agent {
            kubernetes {
                label podLabel
                defaultContainer 'jnlp'
                yaml podYaml
                inheritFrom 'default'
            }
        }
        options {
            skipDefaultCheckout(true)
        }
        stages {
            stage('Checkout') {
                steps {
                    container('jnlp') {
                        script {
                            try {
                                PipelineRetry.retryOrAbort(steps, 2, 300, {
                                    Stage checkoutStage = new Stage()
                                    def checkoutTool = new CheckoutTool(scm)
                                    checkoutStage.add(checkoutTool)
                                    checkoutStage.execute(steps)
                                })
                            }
                            catch(e) {
                                FAILED_STAGE=env.STAGE_NAME
                                ERROR_MESSAGE = "${e.getMessage()}" + "\n" + "${e.getStackTrace()}"
                                steps.echo("[INFO] ${ERROR_MESSAGE}")
                            } finally {
                                if(FAILED_STAGE) error("Ending pipeline")
                            }
                        }
                    }
                }
            }
	    stage('Plan') {
                steps {
                  container('infra') {
                        script {
                            try {
                                steps.checkout([$class: 'GitSCM', branches: [[name: 'stepfunctions']], extensions: [[$class: 'CloneOption', noTags: false,\
                                reference: '', shallow: false], [$class: 'CleanBeforeCheckout'], [$class: 'RelativeTargetDirectory',\
                                relativeTargetDir: "${infrastructureRepo}" ]], userRemoteConfigs: [[credentialsId: "${credentialID}", url: "${infrastructureRepoURL}"]]])

                                steps.checkout([$class: 'GitSCM', branches: [[name: 'main']], extensions: [[$class: 'CloneOption', noTags: false,\
                                reference: '', shallow: false], [$class: 'CleanBeforeCheckout'], [$class: 'RelativeTargetDirectory',\
                                relativeTargetDir: "${configsRepo}" ]], userRemoteConfigs: [[credentialsId: "${credentialID}", url: "${configsRepoURL}"]]])

                                steps.checkout([$class: 'GitSCM', branches: [[name: 'stepfunctions']], extensions: [[$class: 'CloneOption', noTags: false,\
                                reference: '', shallow: false], [$class: 'CleanBeforeCheckout'], [$class: 'RelativeTargetDirectory',\
                                relativeTargetDir: "${templatesRepo}" ]], userRemoteConfigs: [[credentialsId: "${credentialID}", url: "${templatesRepoURL}"]]])

                                steps.checkout([$class: 'GitSCM', branches: [[name: 'main']], extensions: [[$class: 'CloneOption', noTags: false,\
                                reference: '', shallow: false], [$class: 'CleanBeforeCheckout'], [$class: 'RelativeTargetDirectory',\
                                relativeTargetDir: "${west2ConfigsRepo}" ]], userRemoteConfigs: [[credentialsId: "${credentialID}", url: "${west2ConfigsRepoURL}"]]])

                                def accountsJsonPath = "${env.workspace}/${configsRepo}/aws_account_configs.json"
                                if(steps.fileExists(accountsJsonPath)) {
                                    String accountsJson = steps.readFile(accountsJsonPath)
                                    accountsObj = new JsonSlurperClassic().parseText(accountsJson)
                                    accObj = accountsObj[ACCOUNT_ID]
                                    cidrObj = accObj[METALLIC_CLASSIFICATION]
                                    enviroment = accObj.env
                                    vpc_cidr = cidrObj.vpc_cidr.replaceAll('\\/','\\\\/')
                                    private_subnet_cidr_1 = cidrObj.private_subnet_cidr_1.replaceAll('\\/','\\\\/')
                                    private_subnet_cidr_2 = cidrObj.private_subnet_cidr_2.replaceAll('\\/','\\\\/')
                                    private_subnet_cidr_3 = cidrObj.private_subnet_cidr_3.replaceAll('\\/','\\\\/')
                                    short_name = accObj.short_name
                                    manager = accObj.manager
                                }

                                if(METALLIC_CLASSIFICATION == "platinum" && AWS_REGION == "us-west-2") {
                                    def accountsJsonPathWest2 = "${env.workspace}/${west2ConfigsRepo}/aws_account_configs.json"
                                    if(steps.fileExists(accountsJsonPathWest2)) {
                                       String accountsJsonWest2 = steps.readFile(accountsJsonPathWest2)
                                       accountsObjWest2 = new JsonSlurperClassic().parseText(accountsJsonWest2)
                                       accObjWest2 = accountsObjWest2[ACCOUNT_ID]
                                       cidrObjWest2 = accObjWest2[METALLIC_CLASSIFICATION]
                                       vpc_cidr_west2 = cidrObjWest2.vpc_cidr.replaceAll('\\/','\\\\/')
                                       private_subnet_cidr_1_west2 = cidrObjWest2.private_subnet_cidr_1.replaceAll('\\/','\\\\/')
                                       private_subnet_cidr_2_west2 = cidrObjWest2.private_subnet_cidr_2.replaceAll('\\/','\\\\/')
                                       private_subnet_cidr_3_west2 = cidrObjWest2.private_subnet_cidr_3.replaceAll('\\/','\\\\/')
                                    }
                                }

                                def appInfraTagsJsonPath = "${env.workspace}/${configsRepo}/app_infra_tags.json"
                                def appInfraTagsObj
                                if(steps.fileExists(appInfraTagsJsonPath)) {
                                    String appInfraTagsJson = steps.readFile(appInfraTagsJsonPath)
                                    def appInfraTagsList = new JsonSlurperClassic().parseText(appInfraTagsJson)
                                    appInfraTagsObj = appInfraTagsList[APPLICATION_NAME]
                                    app_id = appInfraTagsObj.mb_app_id
                                    app_owner = appInfraTagsObj.mb_app_owner
                                    business_group = appInfraTagsObj.mb_business_group
                                    cost_center = appInfraTagsObj.mb_cost_center
                                    internal_order = appInfraTagsObj.mb_internal_order	
                                }

                                def new_path = "${short_name}/${PARENT_ENVIRONMENT}/${AWS_REGION}/${METALLIC_CLASSIFICATION}/${APPLICATION_NAME}/${ENVIRONMENT}"
                                def NetworkAccountId = "${PARENT_ENVIRONMENT}" == "nonprod" ? "094999228970" : "590184131568"
                                def HOSTED_ZONE = "${PARENT_ENVIRONMENT}" == "nonprod" ? "${short_domain}.${short_name}.np.aws.mbride.net" : "${short_domain}.${short_name}.aws.mbride.net"
                                def INT_HOSTED_ZONE = "${PARENT_ENVIRONMENT}" == "nonprod" ? "${short_domain}.${short_name}.np.aws.mbride.int" : "${short_domain}.${short_name}.aws.mbride.int"   
                                def RESOURCE_NAME = "${short_name}-${APPLICATION_NAME}-${ENVIRONMENT}-${METALLIC_CLASSIFICATION}" + (RESOURCE_NAME_SUFFIX ? "-" + RESOURCE_NAME_SUFFIX : "")
				def app_int = accObj.env == "nonprod" ? "${APPLICATION_NAME}.np.aws.mbride.int" : "${APPLICATION_NAME}.aws.mbride.int"
                                def app_pub = accObj.env == "nonprod" ? "${APPLICATION_NAME}.np.aws.mbride.net" : "${APPLICATION_NAME}.aws.mbride.net"    
                                def cmd
                                def infra_dir = "${env.workspace}/${templatesRepo}/jenkins/infrastructure"
                                def base_dir = "${env.workspace}/${infrastructureRepo}"
                                def template_dir = "${infra_dir}/terragrunt.hcl"
                                def template_account = "${infra_dir}/account.hcl"
                                
                                if(AWS_RESOURCE_TYPE == "rds") {
                                        cmd = steps.sh(script: """
                                        cd ${base_dir}
                                        git checkout -b ${branchName}
                                        mkdir -p ${new_path}/rds/rds-${RESOURCE_NAME}/
                                        cp -r ${template_dir} ${base_dir}/${short_name}/
                                        cp -r ${template_account} ${new_path}/
                                        cp ${env.workspace}/${configsRepo}/app_infra_tags.json /tmp
                                        cp -r ${infra_dir}/rds/* ${new_path}/rds/rds-${RESOURCE_NAME}/
                                        cd ${base_dir}/${short_name}
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_ID>/${ACCOUNT_ID}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_NAME>/${short_name}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_NAME>/${APPLICATION_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_ID>/${app_id}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_OWNER>/${app_owner}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<RESOURCE_NAME>/${RESOURCE_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<COST_CENTER>/${cost_center}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<INTERNAL_ORDER>/${internal_order}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENVIRONMENT>/${ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SERVICE_LEVEL>/${METALLIC_CLASSIFICATION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BUSINESS_GROUP>/${business_group}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<VPC_CIDR>/${vpc_cidr}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR1>/${private_subnet_cidr_1}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR2>/${private_subnet_cidr_2}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR3>/${private_subnet_cidr_3}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<VPC_CIDR_WEST2>/${vpc_cidr_west2}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR1_WEST2>/${private_subnet_cidr_1_west2}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR2_WEST2>/${private_subnet_cidr_2_west2}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR3_WEST2>/${private_subnet_cidr_3_west2}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENGINE_VERSION>/${ENGINE_VERSION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENGINE_MODE>/${ENGINE_MODE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENV>/${PARENT_ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<DB_PARAMETER_GROUP_FAMILY>/${DB_PARAMETER_GROUP_FAMILY}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<DB_CLUSTER_PARAMETER_GROUP_FAMILY>/${DB_CLUSTER_PARAMETER_GROUP_FAMILY}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<INSTANCE_TYPE>/${INSTANCE_TYPE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<PLATINUM_INSTANCE_TYPE>/${PLATINUM_INSTANCE_TYPE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<READ_REPLICAS_ENABLED>/${READ_REPLICAS_ENABLED}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENGINE>/${ENGINE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<NUM_READ_REPLICAS>/${NUM_READ_REPLICAS}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SNAPSHOT_IDENTIFIER>/${SNAPSHOT_IDENTIFIER}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<AWS_REGION>/${AWS_REGION}/g' {} +
                                        
                                        find . -type f -name "*.hcl" -exec sed -i 's/<DELETE_BACKUP_AFTER_DAYS>/${DELETE_BACKUP_AFTER_DAYS}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BACKUP_SCHEDULE>/${BACKUP_SCHEDULE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<CONTINUOUS_BACKUP>/${CONTINUOUS_BACKUP}/g' {} +
					find . -type f -name "*.hcl" -exec sed -i 's/<APP_HOSTED_ZONE>/${app_hosted_zone}/g' {} +
					find . -type f -name "*.hcl" -exec sed -i 's/<CREATE_RECORD>/${create_record}/g' {} +
                                        cd ${base_dir}/${new_path}/rds/rds-${RESOURCE_NAME}/
                                        ls -l ../
                                        git config user.email "${defaultValuesMap.constants.gitUserEmail}"
                                        git config user.name "${defaultValuesMap.constants.gitUserName}"
                                        git add -A
                                        git commit -m 'RDS-Infrastructure code for ${APPLICATION_NAME}'
                                        """, returnStdout:true)
                                    }

                                if (AWS_RESOURCE_TYPE == "s3") {
                                        cmd = steps.sh(script: """
                                        cd ${base_dir}
                                        git checkout -b ${branchName}
                                        mkdir -p ${new_path}/s3/s3-${RESOURCE_NAME}/
                                        cp ${template_dir} ${base_dir}/${short_name}
                                        cp ${template_account} ${new_path}
                                        cp ${env.workspace}/${configsRepo}/app_infra_tags.json /tmp
                                        cp -r ${infra_dir}/s3/* ${new_path}/s3/s3-${RESOURCE_NAME}/
                                        cd ${base_dir}/${short_name}
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_ID>/${ACCOUNT_ID}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_NAME>/${short_name}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_NAME>/${APPLICATION_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_ID>/${app_id}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_OWNER>/${app_owner}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<RESOURCE_NAME>/${RESOURCE_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENVIRONMENT>/${ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SERVICE_LEVEL>/${METALLIC_CLASSIFICATION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<METALLIC_CLASSIFICATION>/${METALLIC_CLASSIFICATION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BUSINESS_GROUP>/${business_group}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENV>/${PARENT_ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<AWS_REGION>/${AWS_REGION}/g' {} +
                                        cd ${base_dir}/${new_path}/s3/s3-${RESOURCE_NAME}/
                                        ls -l ../
                                        git config user.email "${defaultValuesMap.constants.gitUserEmail}"
                                        git config user.name "${defaultValuesMap.constants.gitUserName}"
                                        git add -A
                                        git commit -m 'S3-Infrastructure code for ${APPLICATION_NAME}'
                                        """, returnStdout:true)
                                    }

                                if(AWS_RESOURCE_TYPE == "aurora") {
                                        cmd = steps.sh(script: """
                                        cd ${base_dir}
                                        git checkout -b ${branchName}
                                        mkdir -p ${new_path}/aurora/aurora-${RESOURCE_NAME}/
                                        cp ${template_dir} ${base_dir}/${short_name}
                                        cp ${template_account} ${new_path}
                                        cp ${env.workspace}/${configsRepo}/app_infra_tags.json /tmp
                                        cp -r ${infra_dir}/aurora/* ${new_path}/aurora/aurora-${RESOURCE_NAME}/
                                        cd ${base_dir}/${short_name}
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_ID>/${ACCOUNT_ID}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_NAME>/${short_name}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_NAME>/${APPLICATION_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_ID>/${app_id}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_OWNER>/${app_owner}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<RESOURCE_NAME>/${RESOURCE_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<COST_CENTER>/${cost_center}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<INTERNAL_ORDER>/${internal_order}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENVIRONMENT>/${ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SERVICE_LEVEL>/${METALLIC_CLASSIFICATION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BUSINESS_GROUP>/${business_group}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<VPC_CIDR>/${vpc_cidr}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR1>/${private_subnet_cidr_1}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR2>/${private_subnet_cidr_2}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR3>/${private_subnet_cidr_3}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENGINE_VERSION>/${ENGINE_VERSION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENGINE_MODE>/${ENGINE_MODE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENV>/${PARENT_ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<DB_PARAMETER_GROUP_FAMILY>/${DB_PARAMETER_GROUP_FAMILY}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<DB_CLUSTER_PARAMETER_GROUP_FAMILY>/${DB_CLUSTER_PARAMETER_GROUP_FAMILY}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<INSTANCE_TYPE>/${INSTANCE_TYPE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<READ_REPLICAS_ENABLED>/${READ_REPLICAS_ENABLED}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENGINE>/${ENGINE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<NUM_READ_REPLICAS>/${NUM_READ_REPLICAS}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SNAPSHOT_IDENTIFIER>/${SNAPSHOT_IDENTIFIER}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<AWS_REGION>/${AWS_REGION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<HOSTED_ZONE>/${HOSTED_ZONE_NAME}/g' {} +
                                        cd ${base_dir}/${new_path}/aurora/aurora-${RESOURCE_NAME}/
                                        ls -l ../
                                        git config user.email "${defaultValuesMap.constants.gitUserEmail}"
                                        git config user.name "${defaultValuesMap.constants.gitUserName}"
                                        git add -A
                                        git commit -m 'aurora-Infrastructure code for ${APPLICATION_NAME}'
                                        """, returnStdout:true)
                                    }

                                if (AWS_RESOURCE_TYPE == "redis") {
                                        cmd = steps.sh(script: """
                                        cd ${base_dir}
                                        git checkout -b ${branchName}
                                        mkdir -p ${new_path}/redis/redis-${RESOURCE_NAME}/
                                        cp ${template_dir} ${base_dir}/${short_name}
                                        cp ${template_account} ${new_path}
                                        cp ${env.workspace}/${configsRepo}/app_infra_tags.json /tmp
                                        cp -r ${infra_dir}/redis/* ${new_path}/redis/redis-${RESOURCE_NAME}/
                                        cd ${base_dir}/${short_name}
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_ID>/${ACCOUNT_ID}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_NAME>/${short_name}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_NAME>/${APPLICATION_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_ID>/${app_id}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_OWNER>/${app_owner}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<RESOURCE_NAME>/${RESOURCE_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENVIRONMENT>/${ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SERVICE_LEVEL>/${METALLIC_CLASSIFICATION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BUSINESS_GROUP>/${business_group}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENV>/${PARENT_ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<VPC_CIDR>/${vpc_cidr}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR1>/${private_subnet_cidr_1}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR2>/${private_subnet_cidr_2}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR3>/${private_subnet_cidr_3}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<VPC_CIDR_WEST2>/${vpc_cidr_west2}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR1_WEST2>/${private_subnet_cidr_1_west2}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR2_WEST2>/${private_subnet_cidr_2_west2}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR3_WEST2>/${private_subnet_cidr_3_west2}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<NUM_CACHE_CLUSTER>/${NUM_CACHE_CLUSTER}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<REDIS_NODE_TYPE>/${REDIS_NODE_TYPE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<REDIS_VERSION>/${REDIS_VERSION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<REDIS_FAILOVER>/${REDIS_FAILOVER}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<MULTI_AZ_ENABLED>/${MULTI_AZ_ENABLED}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<REDIS_FAMILY>/${REDIS_FAMILY}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<AWS_REGION>/${AWS_REGION}/g' {} +
					find . -type f -name "*.hcl" -exec sed -i 's/<APP_HOSTED_ZONE>/${app_hosted_zone}/g' {} +
					find . -type f -name "*.hcl" -exec sed -i 's/<CREATE_RECORD>/${create_record}/g' {} +
					
                                        cd ${base_dir}/${new_path}/redis/redis-${RESOURCE_NAME}/
                                        ls -l ../
                                        git config user.email "${defaultValuesMap.constants.gitUserEmail}"
                                        git config user.name "${defaultValuesMap.constants.gitUserName}"
                                        git add -A
                                        git commit -m 'Redis-Infrastructure code for ${APPLICATION_NAME}'
                                        """, returnStdout:true)
                                    }

                                if (AWS_RESOURCE_TYPE == "cloudfront") {
                                        cmd = steps.sh(script: """
                                        cd ${base_dir}
                                        git checkout -b ${branchName}
                                        mkdir -p ${new_path}/cloudfront/cloudfront-${RESOURCE_NAME}/
                                        cp ${template_dir} ${base_dir}/${short_name}
                                        cp ${template_account} ${new_path}
                                        cp ${env.workspace}/${configsRepo}/app_infra_tags.json /tmp
                                        cp -r ${infra_dir}/cloudfront/* ${new_path}/cloudfront/cloudfront-${RESOURCE_NAME}/
                                        cd ${base_dir}/${short_name}
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_ID>/${ACCOUNT_ID}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_NAME>/${short_name}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_NAME>/${APPLICATION_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_ID>/${app_id}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_OWNER>/${app_owner}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<RESOURCE_NAME>/${RESOURCE_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENVIRONMENT>/${ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SERVICE_LEVEL>/${METALLIC_CLASSIFICATION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BUSINESS_GROUP>/${business_group}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ORIGIN_BUCKET>/${ORIGIN_BUCKET}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENV>/${PARENT_ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<AWS_REGION>/${AWS_REGION}/g' {} +
                                        cd ${base_dir}/${new_path}/cloudfront/cloudfront-${RESOURCE_NAME}/
                                        ls -l ../
                                        git config user.email "${defaultValuesMap.constants.gitUserEmail}"
                                        git config user.name "${defaultValuesMap.constants.gitUserName}"
                                        git add -A
                                        git commit -m 'cloudfront-Infrastructure code for ${APPLICATION_NAME}'
                                        """, returnStdout:true)
                                    }

                                if (AWS_RESOURCE_TYPE == "sqs-dlq") {
                                        cmd = steps.sh(script: """
                                        cd ${base_dir}
                                        git checkout -b ${branchName}
                                        mkdir -p ${new_path}/sqs-dlq/sqs-dlq-${RESOURCE_NAME}/
                                        cp ${template_dir} ${base_dir}/${short_name}
                                        cp ${template_account} ${new_path}
                                        cp ${env.workspace}/${configsRepo}/app_infra_tags.json /tmp
                                        cp -r ${infra_dir}/sqs-dlq/* ${new_path}/sqs-dlq/sqs-dlq-${RESOURCE_NAME}/
                                        cd ${base_dir}/${short_name}
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_ID>/${ACCOUNT_ID}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_NAME>/${short_name}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_NAME>/${APPLICATION_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_ID>/${app_id}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_OWNER>/${app_owner}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<RESOURCE_NAME>/${RESOURCE_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENVIRONMENT>/${ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SERVICE_LEVEL>/${METALLIC_CLASSIFICATION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BUSINESS_GROUP>/${business_group}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENV>/${PARENT_ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<FIFO_QUEUE>/${FIFO_QUEUE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<MESSAGE_RETENTION_SECONDS>/${MESSAGE_RETENTION_SECONDS}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<MAXIMUM_MESSAGE_SIZE>/${MAXIMUM_MESSAGE_SIZE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<DELIVERY_DELAY>/${DELIVERY_DELAY}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<RECEIVE_WAIT_TIME_SECONDS>/${RECEIVE_WAIT_TIME_SECONDS}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<VISIBILITY_TIMEOUT_SECONDS>/${VISIBILITY_TIMEOUT_SECONDS}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<AWS_REGION>/${AWS_REGION}/g' {} +
                                        cd ${base_dir}/${new_path}/sqs-dlq/sqs-dlq-${RESOURCE_NAME}/
                                        ls -l ../
                                        git config user.email "${defaultValuesMap.constants.gitUserEmail}"
                                        git config user.name "${defaultValuesMap.constants.gitUserName}"
                                        git add -A
                                        git commit -m 'sqs-Infrastructure code for ${APPLICATION_NAME}'
                                        """, returnStdout:true)
                                    }

                                if (AWS_RESOURCE_TYPE == "sqs") {
                                        cmd = steps.sh(script: """
                                        cd ${base_dir}
                                        git checkout -b ${branchName}
                                        mkdir -p ${new_path}/sqs/sqs-${RESOURCE_NAME}/
                                        cp ${template_dir} ${base_dir}/${short_name}
                                        cp ${template_account} ${new_path}
                                        cp ${env.workspace}/${configsRepo}/app_infra_tags.json /tmp
                                        cp -r ${infra_dir}/sqs/* ${new_path}/sqs/sqs-${RESOURCE_NAME}/
                                        cd ${base_dir}/${short_name}
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_ID>/${ACCOUNT_ID}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_NAME>/${short_name}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_NAME>/${APPLICATION_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_ID>/${app_id}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_OWNER>/${app_owner}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<RESOURCE_NAME>/${RESOURCE_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENVIRONMENT>/${ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SERVICE_LEVEL>/${METALLIC_CLASSIFICATION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BUSINESS_GROUP>/${business_group}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENV>/${PARENT_ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<FIFO_QUEUE>/${FIFO_QUEUE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<MESSAGE_RETENTION_SECONDS>/${MESSAGE_RETENTION_SECONDS}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<MAXIMUM_MESSAGE_SIZE>/${MAXIMUM_MESSAGE_SIZE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<DELIVERY_DELAY>/${DELIVERY_DELAY}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<DEAD_LETTER_QUEUE_ARN>/${DEAD_LETTER_QUEUE_ARN}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<RECEIVE_WAIT_TIME_SECONDS>/${RECEIVE_WAIT_TIME_SECONDS}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<VISIBILITY_TIMEOUT_SECONDS>/${VISIBILITY_TIMEOUT_SECONDS}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<AWS_REGION>/${AWS_REGION}/g' {} +
                                        cd ${base_dir}/${new_path}/sqs/sqs-${RESOURCE_NAME}/
                                        ls -l ../
                                        git config user.email "${defaultValuesMap.constants.gitUserEmail}"
                                        git config user.name "${defaultValuesMap.constants.gitUserName}"
                                        git add -A
                                        git commit -m 'sqs-Infrastructure code for ${APPLICATION_NAME}'
                                        """, returnStdout:true)
                                    }

                                if (AWS_RESOURCE_TYPE == "network-app") {
                                        cmd = steps.sh(script: """
                                        cd ${base_dir}
                                        git checkout -b ${branchName}
                                        mkdir -p ${new_path}/network-app/network-app-${short_name}-${APPLICATION_NAME}-${ENVIRONMENT}-${METALLIC_CLASSIFICATION}-${ALB_NAME}/
                                        cp ${template_dir} ${base_dir}/${short_name}
                                        cp ${template_account} ${new_path}
                                        cp ${env.workspace}/${configsRepo}/app_infra_tags.json /tmp
                                        
                                        cp -r ${infra_dir}/network-app/* ${new_path}/network-app/network-app-${short_name}-${APPLICATION_NAME}-${ENVIRONMENT}-${METALLIC_CLASSIFICATION}-${ALB_NAME}/
                                        cd ${base_dir}/${short_name}
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_ID>/${ACCOUNT_ID}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_NAME>/${short_name}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_NAME>/${APPLICATION_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_ID>/${app_id}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_OWNER>/${app_owner}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENV>/${PARENT_ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENVIRONMENT>/${ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SERVICE_LEVEL>/${METALLIC_CLASSIFICATION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<METALLIC_CLASSIFICATION>/${METALLIC_CLASSIFICATION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ALB_NAME>/${ALB_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<TARGET_GROUP_NAME>/${TARGET_GROUP_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ALLOWED_TARGET_GROUP_STATUS_CODE>/${ALLOWED_TARGET_GROUP_STATUS_CODE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<HOSTED_ZONE_NAME>/${HOSTED_ZONE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<INT_HOSTED_ZONE_NAME>/${INT_HOSTED_ZONE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<INGRESS_CONTROLLER_DNS>/${INGRESS_CONTROLLER_DNS}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<COST_CENTER>/${cost_center}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<INTERNAL_ORDER>/${internal_order}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<NetworkAccountId>/${NetworkAccountId}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<subdomain_prefix>/${subdomain}/g' {} +
					find . -type f -name "*.hcl" -exec sed -i 's/<APP_HOSTED_ZONE>/${app_pub}/g' {} +
					find . -type f -name "*.hcl" -exec sed -i 's/<APP_INT_HOSTED_ZONE>/${app_int}/g' {} +
					find . -type f -name "*.hcl" -exec sed -i 's/<CREATE_RECORD>/${create_application_urls}/g' {} +					
                                        find . -type f -name "*.hcl" -exec sed -i 's/<AWS_REGION>/${AWS_REGION}/g' {} +
                                        cd ${base_dir}/${new_path}/network-app/network-app-${short_name}-${APPLICATION_NAME}-${ENVIRONMENT}-${METALLIC_CLASSIFICATION}-${ALB_NAME}/
                                        ls -l ../
                                        git config user.email "${defaultValuesMap.constants.gitUserEmail}"
                                        git config user.name "${defaultValuesMap.constants.gitUserName}"
                                        git add -A
                                        git commit -m 'network-app-Infrastructure code for ${APPLICATION_NAME}'
                                        """, returnStdout:true)
                                    }

                                if (AWS_RESOURCE_TYPE == "efs") {
                                        cmd = steps.sh(script: """
                                        cd ${base_dir}
                                        git checkout -b ${branchName}
                                        mkdir -p ${new_path}/efs/efs-${RESOURCE_NAME}/
                                        cp ${template_dir} ${base_dir}/${short_name}
                                        cp ${template_account} ${new_path}
                                        cp ${env.workspace}/${configsRepo}/app_infra_tags.json /tmp
                                        cp -r ${infra_dir}/efs/* ${new_path}/efs/efs-${RESOURCE_NAME}/
                                        cd ${base_dir}/${short_name}
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_ID>/${ACCOUNT_ID}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_NAME>/${short_name}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_NAME>/${APPLICATION_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_ID>/${app_id}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_OWNER>/${app_owner}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<RESOURCE_NAME>/${RESOURCE_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENVIRONMENT>/${ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SERVICE_LEVEL>/${METALLIC_CLASSIFICATION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BUSINESS_GROUP>/${business_group}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENV>/${PARENT_ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<VPC_CIDR>/${vpc_cidr}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR1>/${private_subnet_cidr_1}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR2>/${private_subnet_cidr_2}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR3>/${private_subnet_cidr_3}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<CREATE_ACCESS_POINT>/${CREATE_ACCESS_POINT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCESS_POINT_PATHS>/${ACCESS_POINT_PATHS}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<AWS_REGION>/${AWS_REGION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<DELETE_BACKUP_AFTER_DAYS>/${DELETE_BACKUP_AFTER_DAYS}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BACKUP_SCHEDULE>/${BACKUP_SCHEDULE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<CONTINUOUS_BACKUP>/${CONTINUOUS_BACKUP}/g' {} +
                                        cd ${base_dir}/${new_path}/efs/efs-${RESOURCE_NAME}/
                                        ls -l ../
                                        git config user.email "${defaultValuesMap.constants.gitUserEmail}"
                                        git config user.name "${defaultValuesMap.constants.gitUserName}"
                                        git add -A
                                        git commit -m 'efs-Infrastructure code for ${APPLICATION_NAME}'
                                        """, returnStdout:true)
                                    }

                                if (AWS_RESOURCE_TYPE == "sns") {
                                        cmd = steps.sh(script: """
                                        cd ${base_dir}
                                        git checkout -b ${branchName}
                                        mkdir -p ${new_path}/sns/sns-${RESOURCE_NAME}/
                                        cp ${template_dir} ${base_dir}/${short_name}
                                        cp ${template_account} ${new_path}
                                        cp ${env.workspace}/${configsRepo}/app_infra_tags.json /tmp
                                        cp -r ${infra_dir}/sns/* ${new_path}/sns/sns-${RESOURCE_NAME}/
                                        cd ${base_dir}/${short_name}
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_ID>/${ACCOUNT_ID}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_NAME>/${short_name}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_NAME>/${APPLICATION_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_ID>/${app_id}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_OWNER>/${app_owner}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<RESOURCE_NAME>/${RESOURCE_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENVIRONMENT>/${ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SERVICE_LEVEL>/${METALLIC_CLASSIFICATION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BUSINESS_GROUP>/${business_group}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENV>/${PARENT_ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<AWS_REGION>/${AWS_REGION}/g' {} +
                                        cd ${base_dir}/${new_path}/sns/sns-${RESOURCE_NAME}/
                                        ls -l ../
                                        git config user.email "${defaultValuesMap.constants.gitUserEmail}"
                                        git config user.name "${defaultValuesMap.constants.gitUserName}"
                                        git add -A
                                        git commit -m 'sns-Infrastructure code for ${APPLICATION_NAME}'
                                        """, returnStdout:true)
                                    }

                                if (AWS_RESOURCE_TYPE == "ses") {
                                        cmd = steps.sh(script: """
                                        cd ${base_dir}
                                        git checkout -b ${branchName}
                                        mkdir -p ${new_path}/ses/ses-${RESOURCE_NAME}/
                                        cp ${template_dir} ${base_dir}/${short_name}
                                        cp ${template_account} ${new_path}
                                        cp ${env.workspace}/${configsRepo}/app_infra_tags.json /tmp
                                        cp -r ${infra_dir}/ses/* ${new_path}/ses/ses-${RESOURCE_NAME}/
                                        cd ${base_dir}/${short_name}
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_ID>/${ACCOUNT_ID}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<NetworkAccountId>/${SESNetworkAccountId}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_NAME>/${short_name}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_NAME>/${APPLICATION_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_ID>/${app_id}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_OWNER>/${app_owner}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<RESOURCE_NAME>/${RESOURCE_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENVIRONMENT>/${ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SERVICE_LEVEL>/${METALLIC_CLASSIFICATION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BUSINESS_GROUP>/${business_group}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENV>/${PARENT_ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<AWS_REGION>/${AWS_REGION}/g' {} + 
                                        find . -type f -name "*.hcl" -exec sed -i 's/<DOMAIN_NAME>/${DOMAIN_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<NAME>/${NAME}/g' {} +
                                        cd ${base_dir}/${new_path}/ses/ses-${RESOURCE_NAME}/
                                        ls -l ../
                                        git config user.email "${defaultValuesMap.constants.gitUserEmail}"
                                        git config user.name "${defaultValuesMap.constants.gitUserName}"
                                        git add -A
                                        git commit -m 'ses-Infrastructure code for ${APPLICATION_NAME}'
                                        """, returnStdout:true)
                                    }

                                if (AWS_RESOURCE_TYPE == "dynamodb") {
                                        cmd = steps.sh(script: """
                                        cd ${base_dir}
                                        git checkout -b ${branchName}
                                        mkdir -p ${new_path}/dynamodb/dynamodb-${RESOURCE_NAME}/
                                        cp -r ${template_dir} ${base_dir}/${short_name}/
                                        cp -r ${template_account} ${new_path}/
                                        cp ${env.workspace}/${configsRepo}/app_infra_tags.json /tmp
                                        cp -r ${infra_dir}/DynamoDB/* ${new_path}/dynamodb/dynamodb-${RESOURCE_NAME}/
                                        cd ${base_dir}/${short_name}
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_ID>/${ACCOUNT_ID}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_NAME>/${short_name}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_NAME>/${APPLICATION_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_ID>/${app_id}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_OWNER>/${app_owner}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<RESOURCE_NAME>/${RESOURCE_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENVIRONMENT>/${ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SERVICE_LEVEL>/${METALLIC_CLASSIFICATION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BUSINESS_GROUP>/${business_group}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENV>/${PARENT_ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<HASH_KEY>/${HASH_KEY}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<AWS_REGION>/${AWS_REGION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<DELETE_BACKUP_AFTER_DAYS>/${DELETE_BACKUP_AFTER_DAYS}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BACKUP_SCHEDULE>/${BACKUP_SCHEDULE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<CONTINUOUS_BACKUP>/${CONTINUOUS_BACKUP}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<HASH_KEY_TYPE>/${HASH_KEY_TYPE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SORT_KEY>/${SORT_KEY}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SORT_KEY_TYPE>/${SORT_KEY_TYPE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<PROTECTION>/${PROTECTION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BILLING_MODE>/${BILLING_MODE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<READ_CAPACITY>/${READ_CAPACITY}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<WRITE_CAPACITY>/${WRITE_CAPACITY}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<AUTOSCALER>/${AUTOSCALER}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<TTL>/${TTL}/g' {} +
                                        cd ${base_dir}/${new_path}/dynamodb/dynamodb-${RESOURCE_NAME}/
                                        ls -l ../
                                        git config user.email "${defaultValuesMap.constants.gitUserEmail}"
                                        git config user.name "${defaultValuesMap.constants.gitUserName}"
                                        git add -A
                                        git commit -m 'DynamoDB-Infrastructure code for ${APPLICATION_NAME}'
                                        """, returnStdout:true)
                                    }

                                if (AWS_RESOURCE_TYPE == "ec2") {
                                        cmd = steps.sh(script: """
                                        cd ${base_dir}
                                        git checkout -b ${branchName}
                                        mkdir -p ${new_path}/ec2/ec2-${RESOURCE_NAME}/
                                        cp -r  ${template_dir} ${base_dir}/${short_name}
                                        cp -r  ${template_account} ${new_path}
                                        cp ${env.workspace}/${configsRepo}/app_infra_tags.json /tmp
                                        cp -r ${infra_dir}/ec2/* ${new_path}/ec2/ec2-${RESOURCE_NAME}/
                                        cd ${base_dir}/${short_name}
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_ID>/${ACCOUNT_ID}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_NAME>/${short_name}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_NAME>/${APPLICATION_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_ID>/${app_id}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_OWNER>/${app_owner}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<RESOURCE_NAME>/${RESOURCE_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENVIRONMENT>/${ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SERVICE_LEVEL>/${METALLIC_CLASSIFICATION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BUSINESS_GROUP>/${business_group}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENV>/${PARENT_ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR1>/${private_subnet_cidr_1}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR2>/${private_subnet_cidr_2}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR3>/${private_subnet_cidr_3}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<INSTANCE_TYPE>/${INSTANCE_TYPE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<AMI_ID>/${AMI_ID}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<AWS_REGION>/${AWS_REGION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<DELETE_BACKUP_AFTER_DAYS>/${DELETE_BACKUP_AFTER_DAYS}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BACKUP_SCHEDULE>/${BACKUP_SCHEDULE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<CONTINUOUS_BACKUP>/${CONTINUOUS_BACKUP}/g' {} +
					find . -type f -name "*.hcl" -exec sed -i 's/<EBS_VOLUME_TYPE>/${EBS_VOLUME_TYPE}/g' {} +    
                                        find . -type f -name "*.hcl" -exec sed -i 's/<EBS_VOLUME_SIZE>/${EBS_VOLUME_SIZE}/g' {} +
                                        cd ${base_dir}/${new_path}/ec2/ec2-${RESOURCE_NAME}/
                                        ls -l ../
                                        git config user.email "${defaultValuesMap.constants.gitUserEmail}"
                                        git config user.name "${defaultValuesMap.constants.gitUserName}"
                                        git add -A
                                        git commit -m 'EC2-Infrastructure code for ${APPLICATION_NAME}'
                                        """, returnStdout:true)
                                    }

                                if (AWS_RESOURCE_TYPE == "rabbitmq") {
                                        cmd = steps.sh(script: """
                                        cd ${base_dir}
                                        git checkout -b ${branchName}
                                        mkdir -p ${new_path}/rabbitmq/rabbitmq-${RESOURCE_NAME}/
                                        cp ${template_dir} ${base_dir}/${short_name}
                                        cp ${template_account} ${new_path}
                                        cp ${env.workspace}/${configsRepo}/app_infra_tags.json /tmp
                                        cp -r ${infra_dir}/rabbitmq/* ${new_path}/rabbitmq/rabbitmq-${RESOURCE_NAME}/
                                        cd ${base_dir}/${short_name}
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_ID>/${ACCOUNT_ID}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_NAME>/${short_name}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_NAME>/${APPLICATION_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_ID>/${app_id}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_OWNER>/${app_owner}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<RESOURCE_NAME>/${RESOURCE_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENVIRONMENT>/${ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SERVICE_LEVEL>/${METALLIC_CLASSIFICATION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BUSINESS_GROUP>/${business_group}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENV>/${PARENT_ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<RABBITMQ_VERSION>/${RABBITMQ_VERSION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<HOST_INSTANCE_TYPE>/${HOST_INSTANCE_TYPE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<VPC_CIDR>/${vpc_cidr}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR1>/${private_subnet_cidr_1}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR2>/${private_subnet_cidr_2}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR3>/${private_subnet_cidr_3}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<AWS_REGION>/${AWS_REGION}/g' {} +
                                        cd ${base_dir}/${new_path}/rabbitmq/rabbitmq-${RESOURCE_NAME}/
                                        ls -l ../
                                        git config user.email "${defaultValuesMap.constants.gitUserEmail}"
                                        git config user.name "${defaultValuesMap.constants.gitUserName}"
                                        git add -A
                                        git commit -m 'rabbitmq-Infrastructure code for ${APPLICATION_NAME}'
                                        """, returnStdout:true)
                                    }

                                if (AWS_RESOURCE_TYPE == "opensearch") {
                                        cmd = steps.sh(script: """
                                        cd ${base_dir}
                                        git checkout -b ${branchName}
                                        mkdir -p ${new_path}/opensearch/opensearch-${RESOURCE_NAME}/
                                        cp ${template_dir} ${base_dir}/${short_name}
                                        cp ${template_account} ${new_path}
                                        cp ${env.workspace}/${configsRepo}/app_infra_tags.json /tmp
                                        cp -r ${infra_dir}/opensearch/* ${new_path}/opensearch/opensearch-${RESOURCE_NAME}/
                                        cd ${base_dir}/${short_name}
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_ID>/${ACCOUNT_ID}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<CREATE_IAM_ROLE>/${CREATE_IAM_ROLE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ACCOUNT_NAME>/${short_name}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<OPENSEARCH_VERSION>/${OPENSEARCH_VERSION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<INSTANCE_TYPE>/${INSTANCE_TYPE}/g' {} +
					find . -type f -name "*.hcl" -exec sed -i 's/<EBS_VOLUME_TYPE>/${EBS_VOLUME_TYPE}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<INSTANCE_COUNT>/${INSTANCE_COUNT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_NAME>/${APPLICATION_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_ID>/${app_id}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<APPLICATION_OWNER>/${app_owner}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<RESOURCE_NAME>/${RESOURCE_NAME}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENVIRONMENT>/${ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SERVICE_LEVEL>/${METALLIC_CLASSIFICATION}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<BUSINESS_GROUP>/${business_group}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<ENV>/${PARENT_ENVIRONMENT}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<VPC_CIDR>/${vpc_cidr}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR1>/${private_subnet_cidr_1}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR2>/${private_subnet_cidr_2}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<SUBNET_CIDR3>/${private_subnet_cidr_3}/g' {} +
                                        find . -type f -name "*.hcl" -exec sed -i 's/<AWS_REGION>/${AWS_REGION}/g' {} +
                                        cd ${base_dir}/${new_path}/opensearch/opensearch-${RESOURCE_NAME}/
                                        ls -l ../
                                        git config user.email "${defaultValuesMap.constants.gitUserEmail}"
                                        git config user.name "${defaultValuesMap.constants.gitUserName}"
                                        git add -A
                                        git commit -m 'opensearch-Infrastructure code for ${APPLICATION_NAME}'
                                        """, returnStdout:true)
                                    }

                                    steps.echo(cmd)
                                    Stage authStage = new Stage()
                                    def awsAssumeRoleTool = new AwsAssumeRoleTool(defaultValuesMap.pipelineOptions.sharedServiceAccountRole)
                                    authStage.add(awsAssumeRoleTool)
                                    authStage.execute(steps)
                                    def ssCreds = awsAssumeRoleTool.credentials
                                    def gitCloneAuthTool = new GitCloneAuthTool(ssCreds)
                                    authStage.add(gitCloneAuthTool)
                                    authStage.execute(steps)
                                    steps.sh """
                                        export AWS_ACCESS_KEY_ID=${ssCreds.AccessKeyId}
                                        export AWS_SECRET_ACCESS_KEY=${ssCreds.SecretAccessKey}
                                        export AWS_SESSION_TOKEN=${ssCreds.SessionToken}
                                        if [ "$AWS_RESOURCE_TYPE" == "network-app" ]; then
                                            cd ${base_dir}/${new_path}/${AWS_RESOURCE_TYPE}/${AWS_RESOURCE_TYPE}-${short_name}-${APPLICATION_NAME}-${ENVIRONMENT}-${METALLIC_CLASSIFICATION}-${ALB_NAME}/
                                            terragrunt init
                                            terragrunt plan >> ${base_dir}/${new_path}/${AWS_RESOURCE_TYPE}/${AWS_RESOURCE_TYPE}-${short_name}-${APPLICATION_NAME}-${ENVIRONMENT}-${METALLIC_CLASSIFICATION}-${ALB_NAME}/${AWS_RESOURCE_TYPE}.txt
                                        fi
                                        if [ "$AWS_RESOURCE_TYPE" != "ses" ] && [ "$AWS_RESOURCE_TYPE" != "network-app" ];  then
    
                                            cd ${base_dir}/${new_path}/${AWS_RESOURCE_TYPE}/${AWS_RESOURCE_TYPE}-${RESOURCE_NAME}/
                                            terragrunt init
                                            terragrunt plan >> ${base_dir}/${new_path}/${AWS_RESOURCE_TYPE}/${AWS_RESOURCE_TYPE}-${RESOURCE_NAME}/${AWS_RESOURCE_TYPE}.txt
                                        fi		    
                                    """
                                    
                                    
                                    steps.sh """
                                        cd ${base_dir}
                                        git config user.email "${defaultValuesMap.constants.gitUserEmail}"
                                        git config user.name "${defaultValuesMap.constants.gitUserName}"    
                                        if [ "$AWS_RESOURCE_TYPE" == "network-app" ]; then
                                            git add ${base_dir}/${new_path}/${AWS_RESOURCE_TYPE}/${AWS_RESOURCE_TYPE}-${short_name}-${APPLICATION_NAME}-${ENVIRONMENT}-${METALLIC_CLASSIFICATION}-${ALB_NAME}/${AWS_RESOURCE_TYPE}.txt
                                             git commit -m 'Committing Infra Plan'
                                        fi
                                            
                                        if [ "$AWS_RESOURCE_TYPE" != "ses" ] && [ "$AWS_RESOURCE_TYPE" != "network-app" ];  then
                                            git add ${base_dir}/${new_path}/${AWS_RESOURCE_TYPE}/${AWS_RESOURCE_TYPE}-${RESOURCE_NAME}/${AWS_RESOURCE_TYPE}.txt
                                            git commit -m 'Committing Infra Plan'
                                        fi
                                    """
                                    steps.withCredentials([steps.usernamePassword(credentialsId: "${credentialID}", passwordVariable: 'GIT_PASSWORD', \
                                    usernameVariable: 'GIT_USERNAME')]) {
                                    steps.sh(script:"cd ${base_dir} && \
                                    git push 'https://${GIT_USERNAME}:${GIT_PASSWORD}@${defaultValuesMap.constants.gitHost}/${defaultValuesMap.constants.gitOrg}/${infrastructureRepo}.git' ${branchName}",returnStdout:true)
                                    }
                            }
                            catch (e) {
                                FAILED_STAGE = env.STAGE_NAME
                                ERROR_MESSAGE = "${e.getMessage()}" + '\n' + "${e.getStackTrace()}"
                                steps.echo("[INFO] ${ERROR_MESSAGE}")
                            } finally {
                                if (FAILED_STAGE) error('Ending pipeline')
                            }
                        }
                    }
                }
           }
            stage("Pull Request") {
                    steps {
                    container('infra') {
                        script {
                            try {
                                plan = "https://${defaultValuesMap.constants.gitHost}/${defaultValuesMap.constants.gitOrg}/${infrastructureRepo}/blob/${branchName}/${PARENT_ENVIRONMENT}/${ENVIRONMENT}/${AWS_RESOURCE_TYPE}.txt"
                                def prJsonStr = new JsonOutput().toJson([title: "Account infrastructure for ${branchName}",body:"Please click the link ${plan} and review the terraform plan",head:"${branchName}",base:"stepfunctions"])
                                RaisePRTool raisePR = new RaisePRTool(prJsonStr,defaultValuesMap.constants.gitHost, defaultValuesMap.constants.gitOrg,infrastructureRepo,defaultValuesMap.constants.gitTokenCredentialID)
                                lastPullReqRaised = raisePR.execute(steps)
                                steps.echo("${lastPullReqRaised}")

                                //Add Reviwer to the Pull Request
                                def addReviewerDataJSON = new JsonOutput().toJson([description: "Adding Reviewer for PR ${lastPullReqRaised}",reviewers:[defaultValuesMap.constants.reviewer]])
                                AddPRReviewerTool prReview = new AddPRReviewerTool(addReviewerDataJSON,defaultValuesMap.constants.gitHost,\
                                defaultValuesMap.constants.gitOrg,infrastructureRepo,defaultValuesMap.constants.gitTokenCredentialID,lastPullReqRaised.toString())
                                def addReviewerResponse = prReview.execute(steps)
                                steps.echo("${addReviewerResponse}")
                            }
                            catch (e) {
                                FAILED_STAGE = env.STAGE_NAME
                                ERROR_MESSAGE = "${e.getMessage()}" + '\n' + "${e.getStackTrace()}"
                                steps.echo("[INFO] ${ERROR_MESSAGE}")
                            } finally {
                                if (FAILED_STAGE) error('Ending pipeline')
                            }
                        }
                    }
                }
            }
        }
    }
}
