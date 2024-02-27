#!/bin/sh
SUBMODELURL=$1
REGISTRYURL=$2
CONTROLPLANEURL=$3
DATAPLANEURL=$4
EDCKEY=$5
ALLOWEDBPNS=$6

echo "Submodel: $SUBMODELURL"
echo "Registry: $REGISTRYURL"
echo "Controlplane: $CONTROLPLANEURL"
echo "Dataplane: $DATAPLANEURL"
echo "EDC Key: $EDCKEY"
echo "Allowed BPNs: $ALLOWEDBPNS"

pip install -r /opt/scripts/requirements.txt
python /opt/scripts/upload.py -f /opt/scripts/testdata.json -s "$SUBMODELURL" -a "$REGISTRYURL" -edc "$CONTROLPLANEURL" -d "$DATAPLANEURL" -k "$EDCKEY" -p id-3.0-trace --allowedBPNs "$ALLOWEDBPNS" --aas3