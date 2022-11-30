
# Setting Up

### Requirements:

<details>
  <summary>Aws Setup</summary>

### Setting Up AWS Bucket:

1. Setting Up -  https://www.youtube.com/watch?v=i4YFFWcyeFM
2. Getting your Acess keys - https://aws.amazon.com/premiumsupport/knowledge-center/create-access-key/

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


</details>

<details>
  <summary>Ftp Setup</summary>
    Coming Soon
</details>


<details>
  <summary>Adding Release Plugin into your Client</summary>

####  Setting up the release plugin

* First copy the release plugin from my repo to your project here are the places to note
    - https://github.com/Mark7625/Elvarg-Client-Public/tree/master/buildSrc <- Copy this whole dir
    - https://github.com/Mark7625/Elvarg-Client-Public/blob/master/build.gradle.kts
      Comapre the two files adding any missing stuff from my build gradle
* Once you have done this you will need to make the keys to do this go to buildSrc/src/main/kotlin/keys.kt and run the file, this will make 3 files

* Next to update your client all you have to do is find this task
  ![image](https://user-images.githubusercontent.com/72366279/172919101-6155b422-84bf-4d68-84ae-7d6d0c87a3b1.png)
  And run this will Automatically Update and share the files to the public

### DO NOT SHARE THE PRIVATE KEY WITH ANYONE

</details>

##
##### Setting up the Launcher

* Once you have added the release plugin into the client
* Copy your launcher.crt that you made  into /resources/net.runelite.launcher/
* Go into launcher.properties And edit the following 
```kotlin
runelite.type.manifest=https://glacyte.co.uk/testting/ClientManifest.json
```

Should link should go to a json on your webhost or aws that looks like this 
```json
[
    {
    "name": "Normal",
    "main": "net.runelite.client.RuneLite",
    "bootstrap": "https://glacyte.co.uk/testting/normal/bootstrap.json",
    "bootstrapsig": "https://glacyte.co.uk/testting/normal/bootstrap.json.sha256",
    "tooltip": "The Latest most stable Client"
    },
    {
        "name": "Beta",
        "main": "net.runelite.client.RuneLite",
        "bootstrap": "https://glacyte.co.uk/testting/beta/bootstrap.json",
        "bootstrapsig": "https://glacyte.co.uk/testting/beta/bootstrap.json.sha256",
        "tooltip": "This Client may have bugs"
    }
]
```

These are the clients that the users can download, if you only have 1 client it will skip
asking the user and download right away, if you have more then 2 clients it will ask what client they would like to play



- Name: The Name of the client,
- Main: This the main run point of the client EG: [net.runelite.client.RuneLite],
- Bootstrap: This is where the bootstrap file of the this client is located
- Bootstrap Sig: This is where the bootstrap Sig file of the this client is located
- Tooltip: This the tooltip that shows when hovering over the button


### Branding
## Names

1) Go into launcher.properties And edit 'elvarg' and links to your server links and name

```kotlin
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
```

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

## Colors

Inside ColorScheme.java you will where you can edit all your RBG colors

```java
/* The blue color used for the branding's accents */
public static final Color BRAND = new Color(220, 138, 0);

/* The blue color used for the branding's accents, with lowered opacity */
public static final Color BRAND_TRANSPARENT = new Color(220, 138, 0, 120);


public static final Color DARK_GRAY_COLOR = new Color(40, 40, 40);
public static final Color DARKER_GRAY_COLOR = new Color(30, 30, 30);
public static final Color MEDIUM_GRAY_COLOR = new Color(77, 77, 77);

/* The background color of the scrollbar's track */
public static final Color SCROLL_TRACK_COLOR = new Color(25, 25, 25);

/* The color for the red progress bar (used in ge offers, farming tracker, etc)*/
public static final Color PROGRESS_ERROR_COLOR = new Color(230, 30, 30);
```

## Getting your files

Once you have pushed your files to github, github actions will build your launcher you can then find them under the workflow action it runs EG: https://github.com/Elvarg-Community/Runelite-Launcher-rsps/actions/runs/3014844052

# Credits
    - Runelite For the base
    - Spooky For helping me understand the sha stuff
