import urllib.request
import json
import os

architectures = {
    "arm64-v8a": {
        "oid": "cbeb699fdc04756c414b1866fac5fbe30a87fc1b9d78241937ae8dc5757f20fa",
        "size": 21170136
    },
    "armeabi-v7a": {
        "oid": "0fc35f157fbf49eed7d1d0e763100550bbb8d2499c9aeed39ed3c918fdc0610a",
        "size": 14637640
    },
    "x86": {
        "oid": "0755be382aaadfeff2c9e1c7a6dc3b3bb26cc740f151c5ebb6c2735c0976f71c",
        "size": 25931800
    },
    "x86_64": {
        "oid": "9adc4747b8c2b1375defbdefc3f5b14d1ed7e964c799fb1299ef7889f013b536",
        "size": 23552888
    }
}

# Construct the batch request body
objects = []
for arch, info in architectures.items():
    objects.append({
        "oid": info["oid"],
        "size": info["size"]
    })

payload = {
    "operation": "download",
    "transfers": ["basic"],
    "objects": objects
}

url = "https://github.com/TGX-Android/tdlib.git/info/lfs/objects/batch"
req = urllib.request.Request(
    url,
    data=json.dumps(payload).encode('utf-8'),
    headers={
        "Accept": "application/vnd.git-lfs+json",
        "Content-Type": "application/vnd.git-lfs+json"
    },
    method="POST"
)

print("Querying GitHub Git LFS batch API...")
try:
    with urllib.request.urlopen(req) as response:
        res_data = json.loads(response.read().decode('utf-8'))
        
        for obj in res_data.get("objects", []):
            oid = obj.get("oid")
            download_url = obj.get("actions", {}).get("download", {}).get("href")
            
            # Find which architecture this matches
            matching_arch = None
            for arch, info in architectures.items():
                if info["oid"] == oid:
                    matching_arch = arch
                    break
            
            if matching_arch and download_url:
                target_dir = f"app/src/main/jniLibs/{matching_arch}"
                os.makedirs(target_dir, exist_ok=True)
                target_path = f"{target_dir}/libtdjni.so"
                print(f"Downloading {matching_arch}/libtdjni.so from Git LFS...")
                
                # Fetch and write
                urllib.request.urlretrieve(download_url, target_path)
                print(f"Successfully downloaded {matching_arch}/libtdjni.so (Size: {os.path.getsize(target_path)} bytes)")
            else:
                print(f"Could not find download link or matching architecture for oid: {oid}")

except Exception as e:
    print(f"An error occurred: {e}")
