@Library('pipeline-library') _

node("android-build-jdk8") {

    stage('Checkout SCM') { checkout scm }

    stage('Compile') {
        sh "./gradlew clean"
        sh "./gradlew assembleRelease"
    }

    stage('Upload AAR to Nexus') {
        BUILD_TYPE = "RELEASE"
        withCredentials([
                usernamePassword(credentialsId: 'nexusDeployerAccount',
                        passwordVariable: 'NEXUS_PASSWORD',
                        usernameVariable: 'NEXUS_USER')
        ]) {
            sh "./gradlew -DNEXUS_PASSWORD=$NEXUS_PASSWORD -DNEXUS_USER=$NEXUS_USER -DBUILD=$BUILD_TYPE uploadArchives"
        }
    }

    stage('Archive AAR') {
        archiveArtifacts '**/*.aar'
    }
}
