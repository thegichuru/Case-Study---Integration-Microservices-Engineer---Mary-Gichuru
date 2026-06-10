This application is a Spring Boot–based microservice that integrates REST and SOAP APIs to retrieve and persist country 
information. It accepts a country name via a REST endpoint, transforms it into a standardized format, and then interacts 
with an external SOAP service to fetch the corresponding country ISO code and detailed country information such as capital 
city, currency, phone code, and languages.

Before deployment ensure:

1. Kubernetes cluster is available
2. kubectl is configured
3. Docker is installed
4. MySQL database is accessible
5. Cluster has outbound access to the CountryInfo SOAP service


The deployment package includes:

Dockerfile
k8s/
├── namespace.yaml
├── configmap.yaml
├── secret.yaml
├── deployment.yaml
├── service.yaml
└── hpa.yaml

scripts/
├── deploy.sh
└── undeploy.sh

Steps to deploying in kubernetes
1. Build and Package

Build the Spring Boot application:

./gradlew clean bootJar --no-daemon

2. Build the Docker image:

docker build -t case-study:latest .

4. secrets
  create your secret file as shown with example file [secret.example.yaml](k8s/secret.example.yaml) with your database creadentials.
   
3. run the deployment script at ./scripts/deploy.sh latest

or optionally manually deploy by running the commands 
kubectl apply -f k8s/namespace.yaml 
kubectl apply -f k8s/configmap.yaml 
kubectl apply -f k8s/secret.yaml 
kubectl apply -f k8s/deployment.yaml 
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml

4. Health Monitoring

The application exposes Spring Boot Actuator endpoints:

/actuator/health
/actuator/health/readiness
/actuator/health/liveness

which kubernetes can use to determine the 

Readiness Probe → determines if traffic can be routed to the pod
Liveness Probe → determines if the pod should be restarted

5. TroubleShooting
1. Pod Issues

Check pod status:

kubectl -n case-study get pods

Common states:

Status	Meaning
Running	Healthy
Pending	Scheduling issue
CrashLoopBackOff	Application repeatedly crashing
ImagePullBackOff	Image cannot be downloaded

Inspect pod details:

kubectl -n case-study describe pod <pod-name>
2. Application Logs

View recent logs:

kubectl -n case-study logs <pod-name> --tail=200

Follow logs in real-time:

kubectl -n case-study logs -f <pod-name>

Look for:

Spring Boot startup failures
Database connection errors
SOAP integration errors
Configuration issues


