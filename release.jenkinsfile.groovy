@Library('pipeline-library') _

String DEVELOP_BRANCH = 'develop'

node("android-build-jdk8") {

    stage('Checkout SCM') { checkout scm }

    stage('Compile') {
        sh "./gradlew clean assembleRelease"
        sh "./gradlew generateJavadocWebsiteZip"
    }

    stage('Quality') {
        sh 'git rev-parse --abbrev-ref HEAD > branch_name'
        String BRANCH_NAME = readFile 'branch_name'
        if (BRANCH_NAME == DEVELOP_BRANCH) {
            withSonarQubeEnv('sonar') { sh "./gradlew fullCoverageReport sonarqube" }
        } else {
            echo "INFO: Skipping Quality check if not on develop branch..."
        }
    }

    stage('Upload AARs to Nexus') {
        if (env.BRANCH_NAME == "develop") {
            BUILD_TYPE = "SNAPSHOT"
        } else {
            BUILD_TYPE = "RELEASE"
        }
        echo "BUILD_TYPE=$BUILD_TYPE"

        echo 'Entering credentials context...'
        withCredentials([
                usernamePassword(credentialsId: 'nexusDeployerAccount',
                        passwordVariable: 'NEXUS_PASSWORD',
                        usernameVariable: 'NEXUS_USER')
        ]) {
            echo 'Now in credentials context.'
            sh "./gradlew -DNEXUS_PASSWORD=$NEXUS_PASSWORD -DNEXUS_USER=$NEXUS_USER -DBUILD=$BUILD_TYPE uploadArchives"
        }
    }

    stage('Archive AAR and Javadoc website ZIP') {
        archiveArtifacts '**/*.aar'
        archiveArtifacts 'build/website/javadoc-website.zip'
    }
}
