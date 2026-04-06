import os

def remove_bom(file_path):
    with open(file_path, 'r', encoding='utf-8-sig') as f:
        content = f.read()
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

# Remove BOM from problematic files
files_to_fix = [
    'src/main/java/com/techshop/backend/service/Impl/InventoryServiceImpl.java',
    'src/main/java/com/techshop/backend/controller/admin/AdminInventoryController.java'
]

for file_path in files_to_fix:
    if os.path.exists(file_path):
        remove_bom(file_path)
        print(f"Removed BOM from {file_path}")
    else:
        print(f"File not found: {file_path}")