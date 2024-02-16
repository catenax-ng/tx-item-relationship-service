#!/bin/sh

pip install -r /opt/scripts/requirements.txt
python /opt/scripts/upload.py -f /opt/scripts/testdata.json -s https://submodelserver.test -a https://digital-twin-registry.test/semantics/registry/api/v3.0 -edc https://provider-controlplane.test -d https://provider-dataplane.test -k TEST -p id-3.0-trace --allowedBPNs BPNL00000001CRHK BPNL00000001ABCD --aas3