# Setting Up

##### Requirements:
1. Amazon AWS Bukkit or some Hosting Platform, The plugin by defualt uses AWS Bukkit

##### Step 1: Setting up the release plugin

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
- Link = The link to your files EG: [https://ethscape.s3.us-east-2.amazonaws.com/repo/] MAKE SURE TO APPEND THE REPO ON TH END
- disableAWS = If you want your files uploaded automaticlly using AWS (If you click fale you will have to manually upload your files or make another way)

* Next to update your client all you have to do is find this task
![image](https://user-images.githubusercontent.com/72366279/172919101-6155b422-84bf-4d68-84ae-7d6d0c87a3b1.png)
And run this will Automaticlly Update and share the files to the public

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
```
* Build the Project and give out your jar
* Replace any Art of names you wish to like [.runlite -> .servername] and pngs ect
* 
Your updater should now be running



# Credits
    - Runelite For the base
    - Spooky For helping me understand the sha stuff
