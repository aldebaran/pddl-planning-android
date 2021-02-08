@Library('pipeline-library') _

node("android-build-jdk8") {

    stage('Checkout SCM') { checkout scm }

    stage('Compile') {
        sh "./gradlew clean assembleRelease"
    }

    stage('Archive AAR') { archiveArtifacts '**/*.aar' }
}
