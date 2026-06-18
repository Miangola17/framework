
echo "=== Compilation du framework ==="

mkdir -p bin

javac -cp lib/servlet-api.jar -d bin src/mg/framework/FrontController.java src/mg/framework/annotation/Controller.java src/mg/framework/utils/ClassScanner.java

if [ $? -ne 0 ]; then
    echo "ERREUR : La compilation a echoue !"
    exit 1
fi

echo "Compilation OK !"

jar -cf framework.jar -C bin .

echo "=== framework.jar genere avec succes ==="

