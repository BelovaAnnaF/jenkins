timeout(3) {
    node("maven-slave") {

        wrap([$class: 'BuildUser']) {
            currentBuild.description = """
user: $BUILD_USER
branch: $BRANCH
        """
        }

        config = readYaml text: env.YAML_CONFIG

        if (config != null) {
            for(param in config.entrySet()) {
                env.setProperty(param.getKey(), param.getValue())
            }
        }

        stage("UI tests in docker image") {
            sh "docker run -v /root/.m2/repository:/root/.m2/repository -v ./surefire-reports:/home/ubuntu/ui-tests/target/surefire-reports -v ./allure-results:/home/ubuntu/ui-tests/target/allure-results localhost:5005/uitests:${env.getProperty('TEST_VERSION')} ${env.getProperty('PARALLEL')} ${env.getProperty('BROWSER_NAME')} ${env.getProperty('GRID_URL')} ${env.getProperty('BROWSER_VERSION')}"
            //sh "sleep 300"
        }

        stage("Publish Allure Reports") {
            allure([
                    includeProperties: false,
                    jdk: '',
                    properties: [],
                    reportBuildPolicy: 'ALWAYS',
                    results: [[path: './allure-results']]
            ])
        }
    }
}