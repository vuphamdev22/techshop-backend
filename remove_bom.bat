@echo off
powershell -Command "Get-Content 'src\main\java\com\techshop\backend\service\Impl\InventoryServiceImpl.java' -Encoding UTF8 | Out-File -FilePath 'temp_inventory.java' -Encoding UTF8"
move temp_inventory.java "src\main\java\com\techshop\backend\service\Impl\InventoryServiceImpl.java"

powershell -Command "Get-Content 'src\main\java\com\techshop\backend\controller\admin\AdminInventoryController.java' -Encoding UTF8 | Out-File -FilePath 'temp_admin.java' -Encoding UTF8"
move temp_admin.java "src\main\java\com\techshop\backend\controller\admin\AdminInventoryController.java"