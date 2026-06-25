
echo "=== Compilation du framework ==="

mkdir -p bin

javac -cp lib/servlet-api.jar -d bin src/mg/framework/*.java src/mg/framework/annotation/*.java src/mg/framework/utils/*.java src/mg/monapp/controllers/*.java

if [ $? -ne 0 ]; then
    echo "ERREUR : La compilation a echoue !"
    exit 1
fi

echo "Compilation OK !"

jar -cf framework.jar -C bin .

echo "=== framework.jar genere avec succes ==="

