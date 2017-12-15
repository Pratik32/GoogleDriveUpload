A java program for uploading files/folders to Google Drive.

Usage:


Program asks for google Oauth2 credentials,when executing for the 1st time.

You can generate Oauth2 credentials using following guide:
http://www.iperiusbackup.net/en/how-to-enable-google-drive-api-and-get-client-credentials/

for configuring the uploader:

$ java -jar uploader -config

for uploading individual files:

$ java -jar uploader.jar file1 .. filen
  
for uploading folders.

$java -jar uploader.jar -r directory_name


