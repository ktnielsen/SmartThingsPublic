/**
 *  MATRIX ZDB5100
 *
 *  Copyright 2019 Kim T. Nielsen
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
metadata {
	definition (name: "MATRIX ZDB5100", namespace: "Logic Group", author: "Kim T. Nielsen", cstHandler: true) {
		capability "Switch Level"
		capability "Color"

		fingerprint mfr: "0234", prod: "0121", model: "0003", deviceJoinName: "Logic Group MATRIX ZDB5100"
	}


	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		multiAttributeTile(name:"switch", type: "lighting", width: 3, height: 4, canChangeIcon: false) {
        	tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
            	attributeState "off", label: 'Off', action: "on", icon: "https://s3-eu-west-1.amazonaws.com/fibaro-smartthings/dimmer/dimmer0.png", backgroundColor: "#ffffff", nextState:"turningOn"
                attributeState "on", label: 'On', action: "off", icon: "https://s3-eu-west-1.amazonaws.com/fibaro-smartthings/dimmer/dimmer100.png", backgroundColor: "#00a0dc", nextState:"turningOff"
                attributeState "turningOn", label:'Turning On', action:"off", icon:"https://s3-eu-west-1.amazonaws.com/fibaro-smartthings/dimmer/dimmer50.png", backgroundColor:"#00a0dc", nextState:"turningOff"
                attributeState "turningOff", label:'Turning Off', action:"on", icon:"https://s3-eu-west-1.amazonaws.com/fibaro-smartthings/dimmer/dimmer50.png", backgroundColor:"#ffffff", nextState:"turningOn"
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
            	attributeState "level", action:"switch level.setLevel"
			}
		}
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'level' attribute
	// TODO: handle 'colorValue' attribute

}

// handle commands
def setLevel() {
	log.debug "Executing 'setLevel'"
	// TODO: handle 'setLevel' command
}

def setColorValue() {
	log.debug "Executing 'setColorValue'"
	// TODO: handle 'setColorValue' command
}