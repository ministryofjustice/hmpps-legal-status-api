# HTTP Client Requests

This folder contains files suitable for using with the intellij HTTP Client to make HTTP requests against services 
such as the APIs that this service is dependent on. Typical use cases for this are to explore and understand the 
request and responses for these dependant services.

**Because of the way HMPPS Auth applies allowlisting to auth clients, it is not possible to use these HTTP Client
requests for `preprod` or `prod`. See the section below on [making requests to preprod or prod](#making-requests-to-preprod-or-prod)**

## Setup

Obtain API client credentials

- copy `http-client.private.env.json.sample` to a new file `http-client.private.env.json`  
  Do not commit the new file to the repo as it is about to contain secrets.
- obtain the client-creds secrets from the namespace `hmpps-legal-status-api-dev`.

  ```shell
  kubectl -n hmpps-legal-status-dev get secret hmpps-legal-status-api-client-creds -o json | jq '.data | map_values(@base64d)' 
  ```

- fill in the API client credentials in these files: `http-client.private.env.json`

---

## Running

Assuming you have the intellij HTTP Client plugin installed, open one of the `.http` files from this folder, and click
the green 'play' arrow for a given request.


## Further reading
The intellij HTTP Client is well documented, but this was a particularly comprehensive but concise guide:  
[Crafting software - The definitive guide to the IntelliJ IDEA HTTP Client](https://emmanuelvalverderamos.substack.com/p/the-definitive-guide-to-the-intellij)

# Making requests to preprod or prod
To make API requests to preprod or prod there is an alternative approach that uses the namespaces' service-pod.  
A shell script `getLegalStatusData.sh` has been provided for this purpose.

## Setup

You need `kubectl` installed, and your `.kube/config` file correctly setup. Refer to the 
[Cloud Platform documentation](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/getting-started/kubectl-config.html).

## Running

The script will retrieve the necessary secrets, so there is no need to manually get the `preprod` or `prod` secrets or
populate them anywhere.

Run the shell script:
```shell
$ ./getLegalStatusData.sh
```
Follow the prompts to select the desired environment and API call, and provide the data as prompted for.

**Note: Even though the shell script retrieves data from `preprod` and `prod` it will only work if the auth token in
your `.kube/config` allows you access to the namespace. You need to be in the relevant `-live` GitHub group**
