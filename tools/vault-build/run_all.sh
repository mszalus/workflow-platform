set -e
SP="$(dirname "$0")"
python "$SP/build_vault.py"
python "$SP/content_docs.py" > /dev/null
python "$SP/content_services.py"
python "$SP/content_concepts.py"
python "$SP/content_reference.py"
python "$SP/content_index.py"
python "$SP/enrich.py"
python "$SP/check_links.py" | head -12
