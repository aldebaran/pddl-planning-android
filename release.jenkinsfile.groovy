@Library('pipeline-library') _

import com.softbankrobotics.pipeline.ArchiveType
import com.softbankrobotics.pipeline.VersionType

def MASTER_BRANCH = 'master'
def EXPERIMENTAL_BRANCH = 'experimental-'
def DEVELOP_BRANCH = 'develop'

node("android-build-jdk8") {

    stage('Checkout SCM') { checkout scm }

    stage('Compile') {
        sh "./gradlew clean assembleRelease"
        sh "./gradlew generateJavadocWebsiteZip"
    }

    stage('Quality') {
        if (env.BRANCH_NAME == DEVELOP_BRANCH) {
            withSonarQubeEnv('sonar') { sh "./gradlew fullCoverageReport sonarqube" }
        } else {
            echo "INFO: Skipping Quality check if not on develop branch..."
        }
    }

    stage('Upload AAR and Javadoc website ZIP to Nexus') {

        if (env.BRANCH_NAME.startsWith(EXPERIMENTAL_BRANCH)) {
            withCredentials([usernamePassword(
                    credentialsId: 'nexusDeployerAccount',
                    passwordVariable: 'NEXUS_PASSWORD',
                    usernameVariable: 'NEXUS_USER'
            )]) { sh "./gradlew -DNEXUS_PASSWORD=$NEXUS_PASSWORD -DNEXUS_USER=$NEXUS_USER -DBUILD=EXPERIMENTAL uploadArchives" }

        } else {
            withNexus {
                if (env.BRANCH_NAME == DEVELOP_BRANCH) {
                    BUILD_TYPE = "SNAPSHOT"
                } else {
                    BUILD_TYPE = "RELEASE"
                }

                withCredentials([usernamePassword(
                        credentialsId: 'nexusDeployerAccount',
                        passwordVariable: 'NEXUS_PASSWORD',
                        usernameVariable: 'NEXUS_USER'
                )]) { sh "./gradlew -DNEXUS_PASSWORD=$NEXUS_PASSWORD -DNEXUS_USER=$NEXUS_USER -DBUILD=$BUILD_TYPE uploadArchives" }

                String versionName = sh(script: "cat gradle.properties | grep VERSION_NAME | awk -F= '{print \$2}'", returnStdout: true).trim()

                def versionType = VersionType.SNAPSHOT
                if (env.BRANCH_NAME == MASTER_BRANCH) {
                    versionType = VersionType.RELEASE
                }

                def path = archiveClassifier2(ArchiveType.DOC, "pddl-planning-javadoc", versionName, "zip", versionType)
                uploadArchive "build/website/javadoc-website.zip", env.NEXUS_URL, path
            }
        }
    }

    stage('Archive AAR and Javadoc website ZIP') {
        archiveArtifacts '**/*.aar'
        archiveArtifacts 'build/website/javadoc-website.zip'
    }
}
