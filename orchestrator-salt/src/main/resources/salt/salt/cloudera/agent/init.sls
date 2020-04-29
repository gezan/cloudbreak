{%- from 'metadata/settings.sls' import metadata with context %}
{%- from 'cloudera/manager/settings.sls' import cloudera_manager with context %}
{%- set manager_server_fqdn = salt['pillar.get']('hosts')[metadata.server_address]['fqdn'] %}

install-cloudera-manager-agent:
  pkg.installed:
    - failhard: True
    - pkgs:
      - cloudera-manager-daemons
      - cloudera-manager-agent
    - unless:
      - rpm -q cloudera-manager-daemons cloudera-manager-agent

install-psycopg2:
  cmd.run:
    - name: pip install psycopg2==2.7.5 --ignore-installed
    - unless: pip list --no-index | grep -E 'psycopg2.*2.7.5'

replace_server_host:
  file.replace:
    - name: /etc/cloudera-scm-agent/config.ini
    - pattern: "server_host=.*"
    - repl: "server_host={{ manager_server_fqdn }}"
    - unless: grep 'server_host={{ manager_server_fqdn }}' /etc/cloudera-scm-agent/config.ini

{% if cloudera_manager.communication.autotls_enabled == True %}

setup_autotls_token:
  file.line:
    - name: /etc/cloudera-scm-agent/config.ini
    - mode: ensure
    - content: "cert_request_token_file=/etc/cloudera-scm-agent/cmagent.token"
    - after: "use_tls=.*"
    - backup: False

{% endif %}

/opt/scripts/generate-host-id.sh:
  file.managed:
    - makedirs: True
    - source: salt://cloudera/agent/scripts/generate-host-id.sh
    - mode: 744

generate_host_id:
  cmd.run:
    - name: /opt/scripts/generate-host-id.sh
    - shell: /bin/bash

set_service_uids:
  cmd.run:
    - name: /opt/cloudera/cm-agent/service/inituids/set-service-uids.py && echo $(date +%Y-%m-%d:%H:%M:%S) >> /var/log/set-service-uids-executed
    - cwd: /opt/cloudera/cm-agent/service/inituids
    - failhard: True
    - onlyif: test -f /opt/cloudera/cm-agent/service/inituids/set-service-uids.py
    - unless: test -f /var/log/set-service-uids-executed
