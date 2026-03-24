import os
import json
from datetime import datetime
from flask import Flask, request, jsonify
from extractors.csv_extractor import extract_csv_metadata

app = Flask(__name__)


def resolve_file_path(file_path: str) -> str:
    if not file_path:
        return file_path

    if os.path.isabs(file_path) and os.path.isfile(file_path):
        return file_path

    # Try current working directory first.
    cwd_candidate = os.path.abspath(file_path)
    if os.path.isfile(cwd_candidate):
        return cwd_candidate

    # Then try repository root when service runs from python_service/.
    service_dir = os.path.dirname(os.path.abspath(__file__))
    repo_root = os.path.dirname(service_dir)
    repo_candidate = os.path.abspath(os.path.join(repo_root, file_path))
    if os.path.isfile(repo_candidate):
        return repo_candidate

    return file_path

@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        'status': 'ok',
        'timestamp': datetime.utcnow().isoformat() + 'Z'
    }), 200


@app.route('/extract', methods=['POST'])
def extract():
    payload = request.get_json(force=True)
    file_path = payload.get('file_path')
    file_name = payload.get('file_name') or os.path.basename(file_path)
    user = payload.get('uploaded_by', 'unknown')
    uploaded_at = payload.get('uploaded_at')

    if not file_path:
        return jsonify({'error': 'file_path is required'}), 400

    file_path = resolve_file_path(file_path)

    if not os.path.isfile(file_path):
        return jsonify({'error': 'file not found', 'file_path': file_path}), 404

    try:
        metadata = extract_csv_metadata(file_path=file_path, file_name=file_name,
                                        uploaded_by=user, uploaded_at=uploaded_at)

        return jsonify({
            'status': 'success',
            'metadata': metadata,
            'extracted_at': datetime.utcnow().isoformat() + 'Z'
        }), 200

    except Exception as exc:
        return jsonify({
            'status': 'failed',
            'error': str(exc)
        }), 500


if __name__ == '__main__':
    port = int(os.getenv('PYTHON_SERVICE_PORT', 8000))
    app.run(host='0.0.0.0', port=port)
