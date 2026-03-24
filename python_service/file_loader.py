import os
from typing import Dict


def load_file_local(file_path: str) -> str:
    normalized = os.path.abspath(file_path)
    if not os.path.isfile(normalized):
        raise FileNotFoundError(f"Local file not found: {normalized}")
    return normalized


def load_file(file_path: str, storage_type: str = 'local', s3_bucket: str = None, s3_key: str = None) -> str:
    if storage_type == 'local':
        return load_file_local(file_path)

    raise NotImplementedError(f"Storage type '{storage_type}' not implemented")
