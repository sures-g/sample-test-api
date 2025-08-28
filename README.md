# `README.md`

## 🚀 **Sample Spring Boot API**

This project is a sample Spring Boot application that demonstrates a complete CI/CD pipeline, from building a Docker image to deploying it on Google Kubernetes Engine (GKE). It includes a multi-stage Dockerfile for an optimized image and GitHub Actions for automated build, push, and deployment.

## 📦 **Project Components**

  * **`pom.xml`**: A Maven configuration file for a **Spring Boot** application. It includes dependencies for `spring-boot-starter-web` for creating a REST API and `spring-boot-starter-actuator` for production-ready features like health checks. It's configured to use **Java 17**.
  * **`Dockerfile`**: A multi-stage Dockerfile that first builds the application into a JAR file using a Maven base image and then creates a final, lightweight image with only the JRE to run the application.
  * **`.github/workflows/docker-build.yml`**: A GitHub Actions workflow that automates the following steps:
    1.  Checks out the code.
    2.  Builds the Docker image from the `Dockerfile`.
    3.  Pushes the image to **GitHub Container Registry (GHCR)**.
    4.  Authenticates with Google Cloud using a Workload Identity Provider.
    5.  Obtains GKE cluster credentials.
    6.  Deploys the application to the GKE cluster by applying the `deployment.yml` and `service.yml` manifests.
  * **`k8s/deployment.yml`**: A Kubernetes Deployment manifest that defines the desired state for your application's pods. It configures the container to use the image from GHCR, sets up port mapping, and includes **liveness and readiness probes** that check the `/actuator/health` endpoint to ensure the application is running and ready to serve traffic.
  * **`k8s/service.yml`**: A Kubernetes Service manifest that exposes the application to external traffic. It uses a **`LoadBalancer`** type to provision a public IP address, routing traffic from port `80` to the application's container port `8080`.

## 🛠️ **Getting Started**

### Prerequisites

  * **Java 17**: Required to build and run the application locally.
  * **Maven**: Used for dependency management and building the project.
  * **Docker**: For building and running the Docker image locally.
  * **A GitHub repository**: To host the project and enable the GitHub Actions workflow.
  * **A Google Cloud project**: With a GKE cluster configured for the CI/CD pipeline.
  * **GitHub PAT**: A Personal Access Token with `write:packages` permission is required for the GitHub Actions workflow to push images to GHCR.

### Building and Running Locally

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/<your-username>/sample-test-api.git
    cd sample-test-api
    ```
2.  **Build the project**:
    ```bash
    mvn clean install
    ```
3.  **Run the application**:
    ```bash
    java -jar target/sample-test-api-0.0.2.jar
    ```
4.  **Access the API**:
    The application will be accessible at `http://localhost:8080`. The `/actuator/health` endpoint is used for health checks.

### Building and Running with Docker

1.  **Build the Docker image**:
    ```bash
    docker build -t sample-test-api .
    ```
2.  **Run the Docker container**:
    ```bash
    docker run -p 8080:8080 sample-test-api
    ```

## ⚙️ **CI/CD Workflow**

This project uses **GitHub Actions** to automate the build, push, and deployment process.

1.  A **push to the `main` branch** triggers the `docker-build.yml` workflow.
2.  The workflow builds the Docker image and tags it with the **commit SHA**.
3.  It then pushes the image to **GHCR**.
4.  Finally, it connects to your GKE cluster and updates the deployment to use the newly pushed image.

-----

Based on the project configuration files, here are the step-by-step instructions for setting up both the GitHub and Google Cloud Platform (GCP) environments to support the CI/CD pipeline.

---

### 🐙 GitHub Setup

1.  **Create a New GitHub Repository**:
    * Initialize a new, empty GitHub repository for your project.
    * Clone this repository to your local machine.
    * Copy the project files (`Dockerfile`, `pom.xml`, `docker-build.yml`, `deployment.yml`, `service.yml`) into the repository directory.
    * Commit and push these files to the `main` branch of your new repository. This will trigger the GitHub Actions workflow once the GCP setup is complete.

2.  **Configure GitHub Secrets**:
    The `docker-build.yml` file references a GitHub Secret named `PAT` for authenticating to the GitHub Container Registry. You will need to create a **Personal Access Token (PAT)** and add it as a secret to your repository.
    * Go to your GitHub profile settings.
    * Navigate to **Developer settings > Personal access tokens > Tokens (classic)**.
    * Click **Generate new token (classic)**.
    * Give it a descriptive name (e.g., "GHCR_ACCESS").
    * Select the `write:packages` permission under the **packages** scope.
    * Generate the token and copy the value.
    * In your repository, go to **Settings > Secrets and variables > Actions**.
    * Click **New repository secret**.
    * Name the secret `PAT` and paste the token value you copied.

---

### ☁️ Google Cloud Platform (GCP) Setup

The `docker-build.yml` workflow uses **Workload Identity Federation** to authenticate with GCP, which is the most secure method as it avoids using long-lived service account keys.

1.  **Enable Required APIs**:
    * In your GCP project, enable the following APIs from the Google Cloud Console or using the `gcloud` CLI:
        * `gke-gcloud-auth-plugin`
        * `Google Kubernetes Engine API`
        * `Artifact Registry API`
        * `Cloud Build API`
        * `Cloud Resource Manager API`
        * `IAM API`

    # Enable required APIs
    gcloud services enable container.googleapis.com \
        artifactregistry.googleapis.com \
        cloudbuild.googleapis.com \
        cloudresourcemanager.googleapis.com \
        iam.googleapis.com \
        --project=for-tech-practice

2.  **Create a GKE Cluster**:
    * Create a GKE cluster with Workload Identity enabled. The `docker-build.yml` file specifies a cluster named `sample-test` in the `asia-east1` region and a project named `for-tech-practice`. You can create a new cluster with these settings.
    * **Note**: The deployment configuration requires a cluster that supports LoadBalancer services for public access.

3.  **Create a Dedicated Service Account**:
    * Create a new service account that the GitHub Actions workflow will impersonate. The `docker-build.yml` file references a service account named `github-actions-sa` within the `for-tech-practice` project.
    * Assign the necessary IAM roles to this service account to allow it to deploy to your GKE cluster. At a minimum, it will need:
        * `Kubernetes Engine Admin` (`roles/container.admin`)
        * `Workload Identity User` (`roles/iam.workloadIdentityUser`)

4.  **Configure Workload Identity Federation**:
    This step establishes a trust relationship between your GitHub repository and the service account.
    * Go to **IAM & Admin > Workload Identity Federation** in the Google Cloud Console.
    * Create a new pool (e.g., `github-pool`).
    * Add a new provider to the pool, selecting `GitHub` as the identity provider.
    * Configure the provider to trust tokens from your specific GitHub repository.
    * The `docker-build.yml` file uses the following settings, which you'll need to match:
        * **Workload Identity Provider**: `projects/183944825075/locations/global/workloadIdentityPools/github-p/providers/github`
        * **Service Account**: `github-actions-sa@for-tech-practice.iam.gserviceaccount.com`

Setting up Workload Identity Federation in GCP allows your GitHub Actions workflow to securely authenticate with your Google Cloud project without using long-lived service account keys. This process establishes a trust relationship between GitHub and a GCP service account.

Here are the steps to configure Workload Identity Federation:

---

### Step 1: Create a Workload Identity Pool
A workload identity pool is a container for all external identities (like GitHub).
* In the Google Cloud Console, navigate to **IAM & Admin > Workload Identity Federation**.
* Click **Create pool**.
* Give the pool a unique ID and a display name. For this project, you can use an ID like `github-pool`.
* Click **Done**.

### Step 2: Create a Workload Identity Provider
A provider defines the rules for authenticating identities within the pool. In this case, it will be GitHub.
* After creating the pool, click **Add a provider**.
* Select **GitHub** as the provider type.
* The provider configuration will automatically fill in the GitHub-specific details.
* Under **Repository owner**, enter your GitHub username or organization name (e.g., `sures-g`).
* Under **Repository name**, enter the name of your repository (e.g., `sample-test-api`).
* Click **Save**. You will now see the provider details, including the **Workload Identity Provider** path, which you'll copy into your GitHub Actions workflow.

### Step 3: Link the Service Account to the Provider
This is the final step that connects your GitHub identity to the GCP service account you want to use.
* In the Google Cloud Console, navigate to the service account you created earlier (e.g., `github-actions-sa`).
* Go to the **Permissions** tab and click **Grant access**.
* In the **New principals** field, paste the **Principal** value from your Workload Identity Provider. The format is typically `principal://iam.googleapis.com/projects/<project_number>/locations/global/workloadIdentityPools/<pool_id>/subject/repo:<owner>/<repository>`.
* Assign the **Workload Identity User** role (`roles/iam.workloadIdentityUser`) to this principal.
* Save the changes.

The `docker-build.yml` file already contains the correct configuration for Workload Identity. It uses the `google-github-actions/auth@v2` action, which handles the secure authentication process based on the trust relationship you just established. You'll simply need to ensure the `workload_identity_provider` and `service_account` values in your workflow match the resources you created in GCP.


After completing these steps, pushing code to the `main` branch of your GitHub repository should automatically trigger the CI/CD pipeline, building the Docker image, pushing it to GHCR, and deploying the application to your GKE cluster.