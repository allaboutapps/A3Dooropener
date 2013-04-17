#Motivation
We got a intercom system at our new office, but everytime a customer or colleague wants to come in, someone has to stand up and hurry to the opening button. To conquer this problem we used a [Raspberry Pi](http://www.raspberrypi.org/) with an [Apache2 Webserver](http://httpd.apache.org/) and a little servo.

#Setup
##Raspberry Pi  

At first of all we had to get the Pi to boot up, you can find a good tutorial [here](http://elinux.org/RPi_Easy_SD_Card_Setup/)

After the first boot we installed apache2  

     sudo apt-get install apache2

Next copy all the files from this repo into the 'www' folder of apache, in our setup it was  

     /var/www/

After that you should see the door opening website if you open up your browser and type in the IP of your raspberry in your network, for example  

     http://192.168.0.10/


##Pinout  

* Servo:
	* Power - Pin 2
	* Ground - Pin 6
	* Signal - Pin 12

* Led:
	* ***Don't forget a resistor (470 Ohm) between Ground and Cathode!***
	* Cathode - Pin 6
	* Anode: - Pin 22  


##How it works

* Apache is used to have a graphical interface programmed in HTML
* In this index.html is a simple button which triggers a javascript (jquery) clickevent
* The javascript function loads a PHP-Script (scripts/open.php)
* open.php executes a Python-Script (scripts/servo.py) which turns the servo and pressed the door opening button

#License:
[See our BSD 3-Clause License](https://github.com/allaboutapps/A3GridTableView/blob/master/LICENSE.txt)

#Contribute:
Feel free to fork and make pull requests! We are also very happy if you tell us about your app(s) which use this control.  


![aaa - AllAboutApps](https://dl.dropbox.com/u/9934540/aaa/aaaLogo.png "aaa - AllAboutApps")  
[© allaboutapps 2012](http://www.allaboutapps.at)
