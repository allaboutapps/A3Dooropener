#!/usr/bin/python

import RPIO
from RPIO import PWM
from time import sleep

SERVO = 18
LED = 25
BUTTON_PRESSED = 1500
BUTTON_UNPRESSED = 1300

def openDoor():
	
	# Setup ledpin as an output to drive the statusled
	RPIO.setup(LED, RPIO.OUT)
	# Create object of PWMs Servoclass
	servo = PWM.Servo()

	# Light up the statusled
	RPIO.output(LED, True)
	# Turn servo clockwise
	servo.set_servo(SERVO, BUTTON_PRESSED)
	# Wait 2sec
	sleep(2)
	# Turn servo back to neutral
	servo.set_servo(SERVO, BUTTON_UNPRESSED)
	# Turning back takes a while, so sleep 1sec
	sleep(1)
	# Turn off the statsled
	RPIO.output(LED, False)
	# Stop the servo
	servo.stop_servo(SERVO)
		

# Call the openDoor function
openDoor()
