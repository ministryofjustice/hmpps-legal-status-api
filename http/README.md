# HTTP Client Requests

This folder contains files suitable for using with the intellij HTTP Client to make HTTP requests against services 
such as the APIs that this service is dependent on. Typical use cases for this are to explore and understand the 
request and responses for these dependant services.

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