
# Setting Up

### Requirements:
1. Amazon AWS Bucket or some Hosting Platform, The plugin by default uses AWS Bucket

### Setting Up AWS Bucket:

1. Setting Up -  https://www.youtube.com/watch?v=i4YFFWcyeFM
2. Getting your Acess keys - https://aws.amazon.com/premiumsupport/knowledge-center/create-access-key/

####  Step 1: Setting up the release plugin

* First copy the release plugin from my repo to your project here are the places to note
    - https://github.com/Mark7625/Elvarg-Client-Public/tree/master/buildSrc <- Copy this whole dir
    - https://github.com/Mark7625/Elvarg-Client-Public/blob/master/build.gradle.kts 
    Comapre the two files adding any missing stuff from my build gradle
* Once you have done this you will need to make the keys to do this go to buildSrc/src/main/kotlin/keys.kt and run the file, this will make 3 files 

![image](https://user-images.githubusercontent.com/72366279/172917501-1b3d9b77-02e4-408c-af27-8f817dab972e.png)
* Navigate to your userhome and make a folder called .aws inside that make a file called credentials with the following content, you should of got these keys from setting up AWS part
```kotlin
[default]
aws_access_key_id=
aws_secret_access_key=
```
* Next Navigate to buildSrc/src/main/kotlin/Project.kt and fill the following Infomation out
```kotlin
const val bucketName = "test"
const val link = ""
const val disableAWS = false
```
### Key
- Bucketname = The name you named your Bucket
- Link = The link to your files EG: [https://elvarg.s3.us-east-2.amazonaws.com/repo/] MAKE SURE TO APPEND THE REPO ON TH END
- disableAWS = If you want your files uploaded automatically using AWS (If you click false you will have to manually upload your files or make another way)

* Next to update your client all you have to do is find this task
![image](https://user-images.githubusercontent.com/72366279/172919101-6155b422-84bf-4d68-84ae-7d6d0c87a3b1.png)
And run this will Automatically Update and share the files to the public

### DO NOT SHARE THE PRIVATE KEY WITH ANYONE

##### Step 2: Setting up the Launcher

* Copy your launcher.crt into /resources/net.runelite.launcher/
* Go into launcher.properties And edit the following 
```kotlin
runelite.discord.invite=https://runelite.net/redirect/launcher/discord
runelite.wiki.troubleshooting.link=https://runelite.net/redirect/launcher/troubleshooting
runelite.download.link=https://runelite.net/
runelite.bootstrap=link/bootstrap.json
runelite.bootstrapsig=link/bootstrap.json.sha256
runelite.main=net.runelite.client.RuneLite
```
[runelite.main] is the path to the main class you run in In Intellij


### Branding
## Names

1) Go into launcher.properties And edit 'elvarg' and links to your server links and name

``kotlin
runelite.launcher.version=${project.version}  
runelite.discord.invite=**https://runelite.net/redirect/launcher/discord**  
runelite.wiki.troubleshooting.link=**https://runelite.net/redirect/launcher/troubleshooting**  
runelite.dnschange.link=https://1.1.1.1/dns/#setup-instructions  
runelite.download.link=https://**elvarg**.net/download/  
runelite.website=https://**elvarg**.net/  
runelite.bootstrap=https://elvarg.s3.eu-west-2.amazonaws.com/bootstrap.json  
runelite.bootstrapsig=https://elvarg.s3.eu-west-2.amazonaws.com/bootstrap.json.sha256  
runelite.name=**Elvarg**  
runelite.main=net.runelite.client.RuneLite
``

2) Inside Intellij click the root of the project and press CTRL + ALT + R

This will bring this window up

![This is an image](https://i.imgur.com/VqiqIeP.png)

Make sure CC is selected and replace 'Elvarg' with 'MyCoolName' MAKE SURE ITS CAPS E same with the server name

3) Inside Intellij click the root of the project and press CTRL + ALT + R

Make sure CC is selected and replace 'elvarg' with 'myCoolName' MAKE SURE ITS LOWERCASE E same with the server name

![This is an image](https://i.imgur.com/3uXXxbL.png)

## Icons / Images

1. /app.ico [128x128] [Transparent Background]
2. /app_small.bmp [60x60] [White Background]
3. /left.bmp [164x314] [Any Background]
4. /appimage/app.png [128x128]  [Transparent Background]
5. /packer/app.icns https://img2icnsapp.com/how-to-create-the-best-mac-icons/

## Getting your files

Once you have pushed your files to github, github actions will build your launcher you can then find them under the workflow action it runs EG: https://github.com/Elvarg-Community/Runelite-Launcher-rsps/actions/runs/3014844052

# Credits
    - Runelite For the base
    - Spooky For helping me understand the sha stuff
