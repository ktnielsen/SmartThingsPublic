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
metadata 
{
	definition (name: "MATRIX ZDB5100", namespace: "Logic Group", author: "Kim T. Nielsen", cstHandler: true) 
	{
    	capability "Actuator"
        capability "Light"

		capability "Switch"
		capability "Switch Level"
        
        capability "Color Control"
    
        capability "Configuration" // needed for configure() function to set any specific configurations
		
        // Standard (Capability) Attributes:
        attribute "switch", "string"
        attribute "level", "number"
        
        // Custom Attributes:
        attribute "logMessage", "string"        // Important log messages.
        attribute "nightmode", "string"         // 'Enabled' or 'Disabled'.
        attribute "scene", "number"             // ID of last-activated scene.
        
		// Custom commands:
		command "toggleNightmode"
        
		fingerprint mfr: "0234", prod: "0121", model: "0003", deviceJoinName: "MATRIX ZDB5100"
	}


	simulator 
	{
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
		// Multi Tile:
		
        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true)
		{
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") 
			{
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
            }

            tileAttribute ("device.level", key: "SLIDER_CONTROL", range:"(0..100)")
			{
                attributeState "level", action:"setLevel"
            }
                        
             tileAttribute ("device.color", key: "COLOR_CONTROL") 
	         {
    	     	attributeState "color", action: "color control.setColor"
        	 }
            
		
        }
        
         // Other Tiles:
         /*
         controlTile("rgbSelector", "device.color", "color", height: 4, width: 4, inactiveLabel: false) 
         {
    		state "color", action: "color control.setColor"
		}
        */
		standardTile("nightmode", "device.nightmode", decoration: "flat", width: 2, height: 2) 
		{
            state "default", label:'${currentValue}', action:"toggleNightmode", icon:"st.Weather.weather4"
        }
        
        valueTile("scene", "device.scene", decoration: "flat", width: 2, height: 2) 
		{
            state "default", label:'Scene: ${currentValue}'
        }
        
        main(["switch"])
		details([
            "switch",
            //"rgbSelector",
            "nightmode",
            "scene"
        ])
    }

	preferences
	{
		section  // NIGHTMODE:
		{
            input type: "paragraph",
                element: "paragraph",
                title: "NIGHTMODE:",
                description: "Nightmode forces the buttons leds to a specific level (e.g. low-level during the night).\n" +
                    "Nightmode can be enabled/disabled manually using the Nightmode tile, or scheduled below."

            input type: "number",
                name: "configNightmodeLevel",
                title: "Nightmode Level: The buttons led level when nightmode is enabled.",
                range: "1..100",
                required: true

			input type: "number",
                name: "configDaymodeLevel",
                title: "Daymode Level: The buttons led level when nightmode is disabled (day mode).",
                range: "1..100",
                required: true

            input type: "time",
                name: "configNightmodeStartTime",
                title: "Nightmode Start Time: Nightmode will be enabled every day at this time.",
                required: false

            input type: "time",
                name: "configNightmodeStopTime",
                title: "Nightmode Stop Time: Nightmode will be disabled every day at this time.",
                required: false
        }
		
		generatePreferenceParameters()
	}
}

/**
 *  parse()
 *
 *  Called when messages from the device are received by the hub. The parse method is responsible for interpreting
 *  those messages and returning event definitions (and command responses).
 *
 *  As this is a Z-wave device, zwave.parse() is used to convert the message into a command. The command is then
 *  passed to zwaveEvent(), which is overloaded for each type of command below.
 *
 *  Parameters:
 *   String      description        The raw message from the device.
 **/
 def parse(String description) 
 {
	log.debug "MATRIX: Parsing '${description}'"
	def result = null
    def cmd = zwave.parse(description)
    if (cmd) 
	{
		result = zwaveEvent(cmd)

        log.debug "Parsed ${cmd} to ${result.inspect()}"
    } 
	else 
	{
		log.debug "Non-parsed event: ${description}"
    }
    return result
}

/*****************************************************************************************************************

 *  Z-wave Event Handlers.

 *****************************************************************************************************************/

/**
 * MultiChannelCmdEncap and MultiInstanceCmdEncap are ways that devices
 * can indicate that a message is coming from one of multiple subdevices
 * or "endpoints" that would otherwise be indistinguishable
 **/
def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) 
{
 	def encapsulatedCommand = cmd.encapsulatedCommand(getCommandClassVersions())

    if (!encapsulatedCommand) 
	{
        log.debug("zwaveEvent(): Could not extract command from ${cmd}")
    }
	else 
	{
        log.debug ("Command from endpoint ${cmd.sourceEndPoint}: ${encapsulatedCommand}")

        return zwaveEvent(encapsulatedCommand)
    }
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) 
{
	log.debug("MATRIX: Basic Report: value = ${cmd.value}")
	def result = []
	result << createEvent(name: "switch", value: cmd.value ? "on" : "off")
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.centralscenev1.CentralSceneNotification cmd) 
{
	log.debug("MATRIX: Central Scene Event, sceneNumber: ${cmd.sceneNumber}, keyAttributes: ${cmd.keyAttributes}")

	def result = []
    result << createEvent(name: "scene", value: "$cmd.sceneNumber", data: [keyAttributes: "$cmd.keyAttributes"], descriptionText: "Scene number ${cmd.sceneNumber} was activated", isStateChange: true)
    
    log.debug("Scene #${cmd.sceneNumber} was activated.")

    return result    
}

/**
 *  zwaveEvent( COMMAND_CLASS_SWITCH_MULTILEVEL V3 (0x26) : SWITCH_MULTILEVEL_REPORT )
 *
 *  The Switch Multilevel Report is used to advertise the status of a multilevel device.
 *
 *  Action: Pass command to dimmerEvent().
 *
 *  cmd attributes:
 *    Short    value
 *      0x00       = Off
 *      0x01..0x63 = 0..100%
 *      0xFE       = Unknown
 *      0xFF       = On [Deprecated]
 *
 *  Example: SwitchMultilevelReport(value: 1)
 **/
def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) 
{
    log.debug("zwaveEvent(): Switch Multilevel Report received: ${cmd}")
    
    return dimmerEvent(cmd)
}

/**
 *  zwaveEvent( COMMAND_CLASS_CONFIGURATION V2 (0x70) : CONFIGURATION_REPORT )
 *
 *  The Configuration Report Command is used to advertise the actual value of the advertised parameter.
 *
 *  Action: Store the value in the parameter cache.
 *
 **/
def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) 
{
    log.debug("zwaveEvent(): Configuration Report received: ${cmd}")

    state."paramCache${cmd.parameterNumber}" = cmd.scaledConfigurationValue.toInteger()
    def paramName = getParametersMetadata().find( { it.id == cmd.parameterNumber }).name
    log.debug("Parameter #${cmd.parameterNumber} [${paramName}] has value: ${cmd.scaledConfigurationValue}")
}

/**
 *  zwaveEvent( COMMAND_CLASS_VERSION V1 (0x86) : VERSION_REPORT )
 *
 *  The Version Report Command is used to advertise the library type, protocol version, and application version.

 *  Action: Publish values as device 'data' and log an info message. No check is performed.
 *
 **/
def zwaveEvent(physicalgraph.zwave.commands.versionv1.VersionReport cmd) 
{
    log.debug("zwaveEvent(): Version Report received: ${cmd}")

    def zWaveLibraryTypeDisp  = String.format("%02X",cmd.zWaveLibraryType)
    
    def applicationVersionDisp = String.format("%d.%02d",cmd.applicationVersion,cmd.applicationSubVersion)
    def zWaveProtocolVersionDisp = String.format("%d.%02d",cmd.zWaveProtocolVersion,cmd.zWaveProtocolSubVersion)

    log.debug("Version Report: Application Version: ${applicationVersionDisp}, " +
           "Z-Wave Protocol Version: ${zWaveProtocolVersionDisp}, " +
           "Z-Wave Library Type: ${zWaveLibraryTypeDisp}")

    updateDataValue("applicationVersion","${cmd.applicationVersion}")
    updateDataValue("applicationSubVersion","${cmd.applicationSubVersion}")
    updateDataValue("zWaveLibraryType","${zWaveLibraryTypeDisp}")
    updateDataValue("zWaveProtocolVersion","${cmd.zWaveProtocolVersion}")
    updateDataValue("zWaveProtocolSubVersion","${cmd.zWaveProtocolSubVersion}")
}

/**
 *  dimmerEvent()
 *
 *  Common handler for BasicReport, SwitchBinaryReport, SwitchMultilevelReport.
 *
 *  Action: Raise 'switch' and 'level' events.
 *   Restore pending level if dimmer has been switched on after nightmode has been disabled.
 *   If Proactive Reporting is enabled, and the level has changed, request a meter report.
 **/
def dimmerEvent(physicalgraph.zwave.Command cmd) 
{
    def result = []

    // switch event:
    def switchValue = (cmd.value ? "on" : "off")
    def switchEvent = createEvent(name: "switch", value: switchValue)
    if (switchEvent.isStateChange) log.debug("Dimmer turned ${switchValue}.")
    result << switchEvent

    // level event:
    def levelValue = Math.round (cmd.value * 100 / 99)
    def levelEvent = createEvent(name: "level", value: levelValue, unit: "%")
    if (levelEvent.isStateChange) 
		log.debug("Dimmer level is ${levelValue}%")
    result << levelEvent

    return result
}


/*****************************************************************************************************************
 *  Capability-related Commands:
 *****************************************************************************************************************/

/**
 *  on()                        [Capability: Switch]
 *
 *  Turn the dimmer on.
 **/
def on() 
{
	def result = []
	result << endpointCmd(zwave.basicV1.basicSet(value: 255), 5).format()
	return response(delayBetween(result, 100))
}

/**
 *  off()                       [Capability: Switch]
 *
 *  Turn the dimmer off.
 **/
def off() 
{
	def result = []
	result << endpointCmd(zwave.basicV1.basicSet(value: 0), 5).format()
	return response(delayBetween(result, 100))
}

/**
 *  setLevel()                  [Capability: Switch Level]
 *
 *  Set the dimmer level.
 *
 *  Parameters:
 *   level    Target level (0-100%).
 **/
def setLevel(level) 
{
    log.debug("setLevel(${level})")
    
    if (level < 0) level = 0
    if (level > 100) level = 100
    
    log.debug("Setting dimmer to ${level}%")

	// Clear nightmodePendingLevel as it's been overridden.
    state.nightmodePendingLevel = 0

	// Convert from 0-100 to 0-99
	level = Math.round(level * 99 / 100) 

    def result = []
	result << endpointCmd(zwave.switchMultilevelV3.switchMultilevelSet(value: level), 5).format() 
    return response(delayBetween(result, 1500))
}

def setColor(value) 
{
	log.debug("MATRIX: setColor(${value})")
    
	def result = []
	if (value.hex) 
    {
    	def c = value.hex.findAll(/[0-9a-fA-F]{2}/).collect { Integer.parseInt(it, 16) }
		
        log.debug("Color: R:${c[0]}, G:${c[1]}, B:${c[2]}")
        
		result << endpointCmd(zwave.switchColorV3.switchColorSet(warmWhite: 0x10, red:c[0], green:c[1], blue:c[2]), 1).format()
        result << endpointCmd(zwave.switchColorV3.switchColorSet(warmWhite: 0x10, red:c[0], green:c[1], blue:c[2]), 2).format()
        result << endpointCmd(zwave.switchColorV3.switchColorSet(warmWhite: 0x10, red:c[0], green:c[1], blue:c[2]), 3).format()
        result << endpointCmd(zwave.switchColorV3.switchColorSet(warmWhite: 0x10, red:c[0], green:c[1], blue:c[2]), 4).format()
	} 
    else
    {
    	def hue = value.hue ?: device.currentValue("hue")
		def saturation = value.saturation ?: device.currentValue("saturation")
		if(hue == null) hue = 13
		if(saturation == null) saturation = 13
		def rgb = huesatToRGB(hue, saturation)
		
        result << endpointCmd(zwave.switchColorV3.switchColorSet(warmWhite: 0x10, red:c[0], green:c[1], blue:c[2]), 1).format()
        result << endpointCmd(zwave.switchColorV3.switchColorSet(warmWhite: 0x10, red:c[0], green:c[1], blue:c[2]), 2).format()
        result << endpointCmd(zwave.switchColorV3.switchColorSet(warmWhite: 0x10, red:c[0], green:c[1], blue:c[2]), 3).format()
        result << endpointCmd(zwave.switchColorV3.switchColorSet(warmWhite: 0x10, red:c[0], green:c[1], blue:c[2]), 4).format()
	}
	
	if (value.hue != null) 
    	sendEvent(name: "hue", value: value.hue)
        
	if (value.hex) 
    	sendEvent(name: "color", value: value.hex)
        
	if (value.saturation != null) 
    	sendEvent(name: "saturation", value: value.saturation)
	
	def lastColor = device.latestValue("color")
	def lastRGB = lastColor.findAll(/[0-9a-fA-F]{2}/).collect { Integer.parseInt(it, 16) }
	
	return response(delayBetween(result, 200))
}

/**
 *  enableNightmode(level)
 *
 *  Starts nightmode and set level to configNightmodeLevel
 *
 **/
def enableNightmode(level=-1) 
{
    // Clean level value:
    if (level == -1) level = settings.configNightmodeLevel.toInteger()
    if (level > 100) level = 100
    if (level < 1) level = 1

	log.debug("enableNightmode(): Setting level: ${level}")

	// convert from 0 - 100 to 0 - 255
	def v = Math.round (level * 255 / 100)
    
	def commands = []
	commands << endpointCmd(zwave.switchColorV3.switchColorSet(warmWhite: 64, red: v, green: v, blue: v), 1).format()
	commands << endpointCmd(zwave.switchColorV3.switchColorSet(warmWhite: 64, red: v, green: v, blue: v), 2).format()
    commands << endpointCmd(zwave.switchColorV3.switchColorSet(warmWhite: 64, red: v, green: v, blue: v), 3).format()
    commands << endpointCmd(zwave.switchColorV3.switchColorSet(warmWhite: 64, red: v, green: v, blue: v), 4).format()

    sendHubCommand(commands.collect{ response(it) }, 200)
	
    state.nightmodeActive = true
    sendEvent(name: "nightmode", value: "Enabled", descriptionText: "Nightmode Enabled", isStateChange: true)
}

/**
 *  disableNightmode()
 *
 *  Stop nightmode and set level to configDaymodeLevel
 *
 *  triggered by schedule().
 **/
 def disableNightmode(level=-1)
 {
    // Clean level value:
    if (level == -1) level = settings.configDaymodeLevel.toInteger()
    if (level > 100) level = 100
    if (level < 1) level = 1

     log.debug("disableNightmode(): Setting level: ${level}")

     // convert from 0 - 100 to 0 - 255
     def v = Math.round (level * 255 / 100)

     def commands = []
     commands << endpointCmd(zwave.switchColorV3.switchColorSet(warmWhite: 64, red: v, green: v, blue: v), 1).format()
     commands << endpointCmd(zwave.switchColorV3.switchColorSet(warmWhite: 64, red: v, green: v, blue: v), 2).format()
     commands << endpointCmd(zwave.switchColorV3.switchColorSet(warmWhite: 64, red: v, green: v, blue: v), 3).format()
     commands << endpointCmd(zwave.switchColorV3.switchColorSet(warmWhite: 64, red: v, green: v, blue: v), 4).format()

     sendHubCommand(commands.collect{ response(it) }, 200)

    state.nightmodeActive = false
    sendEvent(name: "nightmode", value: "Disabled", descriptionText: "Nightmode Disabled", isStateChange: true)
}

/**
 *  toggleNightmode()
 **/
def toggleNightmode() 
{
    log.debug("toggleNightmode()")

    if (state.nightmodeActive) 
	{
	    sendEvent(name: "nightmode", value: "Disabled", descriptionText: "Nightmode Disabled", isStateChange: true)

        disableNightmode(configDaymodeLevel)
    }
    else 
	{
	    sendEvent(name: "nightmode", value: "Enabled", descriptionText: "Nightmode Enabled", isStateChange: true)

        enableNightmode(configNightmodeLevel)
    }
}


/**
 * Configuration capability command handler.
 *
 * @param void
 * @return List of commands that will be executed in sequence with 500 ms delay inbetween.
*/
def configure() 
{
	log.debug "MATRIX: configure()"
	/*
	def cmds = []
	cmds << zwave.associationV1.associationRemove(groupingIdentifier:1, nodeId:zwaveHubNodeId).format()
	cmds << zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier: 1, nodeId: [0,zwaveHubNodeId,1]).format()
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 10, size: 1, scaledConfigurationValue: 0x01).format()
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 14, size: 4, scaledConfigurationValue: 0xFF555500).format()
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 22, size: 4, scaledConfigurationValue: 0x0000FF00).format()
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 23, size: 4, scaledConfigurationValue: 0x7F7F7F00).format()
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 30, size: 4, scaledConfigurationValue: 0x0000FF00).format()
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 31, size: 4, scaledConfigurationValue: 0x7F7F7F00).format()
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 38, size: 4, scaledConfigurationValue: 0x0000FF00).format()
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 39, size: 4, scaledConfigurationValue: 0x7F7F7F00).format()
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 46, size: 4, scaledConfigurationValue: 0x0000FF00).format()
    cmds << zwave.configurationV1.configurationSet(parameterNumber: 47, size: 4, scaledConfigurationValue: 0x7F7F7F00).format()

	return response(delayBetween(cmds, 1000))
	*/
}

/**
 *  sync()
 *
 *  Manages synchronisation of parameters, association groups, and protection state with the physical device.
 *  The syncPending attribute advertises remaining number of sync operations.
 *
 *  Does not return a list of commands, it sends them immediately using sendSecureSequence(). This is required if
 *  triggered by schedule().
 *
 *  Parameters:
 *   forceAll    Force all items to be synced, otherwise only changed items will be synced.
 **/
private sync(forceAll = false) 
{
    log.debug("sync(): Syncing configuration with the physical device.")

    def cmds = []

    if (forceAll) // Clear all cached values.
	{
        getParametersMetadata().findAll( {!it.readonly} ).each { state."paramCache${it.id}" = null }
        //getAssocGroupsMd().each { state."assocGroupCache${it.id}" = null }
    }

    getParametersMetadata().findAll( {!it.readonly} ).each // Exclude readonly parameters.
	{
        if ((state."paramTarget${it.id}" != null) & (state."paramCache${it.id}" != state."paramTarget${it.id}"))
		{
            cmds << zwave.configurationV2.configurationSet(parameterNumber: it.id, size: it.size, scaledConfigurationValue: state."paramTarget${it.id}".toInteger())
            cmds << zwave.configurationV2.configurationGet(parameterNumber: it.id)
            log.debug("sync(): Syncing parameter #${it.id} [${it.name}]: New Value: " + state."paramTarget${it.id}")
        }
    }

	/*
    getAssocGroupsMd().each {
        def cachedNodes = state."assocGroupCache${it.id}"
        def targetNodes = state."assocGroupTarget${it.id}"

        if ( cachedNodes != targetNodes ) {
            // Display to user in hex format (same as IDE):
            def targetNodesHex  = []
            targetNodes.each { targetNodesHex.add(String.format("%02X", it)) }
            logger("sync(): Syncing Association Group #${it.id}: Destinations: ${targetNodesHex}","info")

            cmds << zwave.multiChannelAssociationV2.multiChannelAssociationRemove(groupingIdentifier: it.id, nodeId: []) // Remove All
            cmds << zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier: it.id, nodeId: targetNodes)
            cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier: it.id)
            syncPending++
        }
    }
	*/

	sendHubCommand(cmds.collect{ response(it) }, 1000) // Need a delay of at least 1000ms.
}

/**
 *  refreshConfig()
 *
 *  Request configuration reports from the physical device: [ Configuration, Association, Version, etc. ]
 *
 *  Really only needed at installation or when debugging, as sync will request the necessary reports when the
 *  configuration is changed.
 */
private refreshConfig() 
{
    log.debug("refreshConfig()")

    def cmds = []

    getParamsMd().each { cmds << zwave.configurationV2.configurationGet(parameterNumber: it.id) }
    //getAssocGroupsMd().each { cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier: it.id) }

    cmds << zwave.versionV1.versionGet()

	sendHubCommand(cmds.collect{ response(it) }, 1000) // Need a delay of at least 1000ms.
}

/**
 *  updated()
 *
 *  Runs when the user hits "Done" from Settings page.
 *
 *  Action: Process new settings, sync parameters and association group members with the physical device. Request
 *  Firmware Metadata, Manufacturer-Specific, and Version reports.
 *
 *  Note: Weirdly, update() seems to be called twice. So execution is aborted if there was a previous execution
 *  within two seconds.
 **/
def updated() 
{
    log.debug("updated()")

    def cmds = []

    if (!state.updatedLastRanAt || now() >= state.updatedLastRanAt + 2000) 
	{
        state.updatedLastRanAt = now()

        // Manage Schedules:
        manageSchedules()

        // Update Parameter target values:
        getParametersMetadata().findAll( {!it.readonly} ).each  // Exclude readonly parameters.
		{
            state."paramTarget${it.id}" = settings."configParam${it.id}"?.toInteger()
        }

        // Update Assoc Group target values:
		/*
        state.assocGroupTarget1 = [ zwaveHubNodeId ] // Assoc Group #1 is Lifeline and will contain controller only.
        getAssocGroupsMd().findAll( { it.id != 1} ).each {
            state."assocGroupTarget${it.id}" = parseAssocGroupInput(settings."configAssocGroup${it.id}", it.maxNodes)
        }
		*/
        
        // Sync configuration with phyiscal device:
        sync(true)

        // Request device medadata (this just seems the best place to do it):
        cmds << zwave.versionV1.versionGet().format()

        return response(delayBetween(cmds, 1000))
    }
    else 
	{
        log.debug("updated(): Ran within last 2 seconds so aborting.")
    }
}

/*****************************************************************************************************************
 *  Private Helper Functions:
 *****************************************************************************************************************/

/**
 *  endpointCmd(cmd, endpoint)
 *
 *  Encapsulate command using multiChannelCmdEncap.
 *  
 **/
private endpointCmd(physicalgraph.zwave.Command cmd, endpoint)
{
	return zwave.multiChannelV3.multiChannelCmdEncap(sourceEndPoint:1, destinationEndPoint:endpoint).encapsulate(cmd)
}

private huesatToRGB(float hue, float sat) 
{
	while(hue >= 100) 
    	hue -= 100
        
	int h = (int)(hue / 100 * 6)
	float f = hue / 100 * 6 - h
	int p = Math.round(255 * (1 - (sat / 100)))
	int q = Math.round(255 * (1 - (sat / 100) * f))
	int t = Math.round(255 * (1 - (sat / 100) * (1 - f)))
	switch (h) 
    {
		case 0: return [255, t, p]
		case 1: return [q, 255, p]
		case 2: return [p, 255, t]
		case 3: return [p, q, 255]
		case 4: return [t, p, 255]
		case 5: return [255, p, q]
	}
}

/**
 *  manageSchedules()
 *
 *  Schedules/unschedules Nightmode.
 **/
private manageSchedules() 
{
    log.debug("manageSchedules()")

    if (configNightmodeStartTime) 
	{
        schedule(configNightmodeStartTime, enableNightmode)
        log.debug("manageSchedules(): Nightmode scheduled to start at ${configNightmodeStartTime}")
    } 
	else 
	{
        try 
		{
            unschedule("enableNightmode")
        }
        catch(e) 
		{
            // Unschedule failed
        }
    }

    if (configNightmodeStopTime) 
	{
        schedule(configNightmodeStopTime, disableNightmode)
        log.debug("manageSchedules(): Nightmode scheduled to stop at ${configNightmodeStopTime}")
    } 
	else
	{
        try 
		{
            unschedule("disableNightmode")
        }
        catch(e) 
		{
            // Unschedule failed
        }
    }
}

/**
 *  generatePreferenceParameters()
 *
 *  Generates preferences (settings) for device parameters.
 **/
private generatePreferenceParameters() 
{
    section 
	{
        input (
			type: "paragraph",
            element: "paragraph",
			title: "CONFIGURATION PARAMETERS:",
            description: "Configuration parameter settings. " +
				"Refer to the product documentation for a full description of each parameter."
        )

		getParametersMetadata().findAll( {!it.readonly} ).each // Exclude readonly parameters.
		{
			def lb = (it.description.length() > 0) ? "\n" : ""

			switch (it.type) 
			{
				case "number":
					input (
						name: "configParam${it.id}",
						title: "#${it.id}: ${it.name}: \n" + it.description + lb +"Default Value: ${it.defaultValue}",
						type: it.type,
						range: it.range,
						required: it.required
					)
					break

				case "enum":
					input (
						name: "configParam${it.id}",
						title: "#${it.id}: ${it.name}: \n" + it.description + lb + "Default Value: ${it.defaultValue}",
						type: it.type,
						options: it.options,
						required: it.required
					)
					break
			}
		}
    }
}

/*****************************************************************************************************************
 *  Static metadata functions:
 *
 *  These functions encapsulate metadata about the device.
 *****************************************************************************************************************/

/**
 *  getCommandClassVersions()
 *
 *  Returns a map of the command class versions supported by the device. Used by parse() and zwaveEvent() to
 *  extract encapsulated commands from MultiChannelCmdEncap and SecurityMessageEncapsulation
 *
 *  Reference: https://products.z-wavealliance.org/products/3399/classes
 **/
 private getCommandClassVersions() 
{
    return [0x20: 1, // Basic V1
            0x26: 3, // Switch Multilevel V3
            0x5B: 1, // Central Scene V3
            0x59: 1, // Association Group Information V1 (Not handled, as no need)
            0x5A: 1, // Device Reset Locally V1
            0x60: 3, // Multi Channel V4 (Device supports V4, but SmartThings only supports V3)
            0x70: 2, // Configuration V2
            0x72: 2, // Manufacturer Specific V2
            0x73: 1, // Powerlevel V1
            0x7A: 2, // Firmware Update MD V3 (Device supports V3, but SmartThings only supports V2)
            0x85: 2, // Association V2
            0x86: 1, // Version V2 (Device supports V2, but SmartThings only supports V1)
            0x8E: 2, // Multi Channel Association V3 (Device supports V3, but SmartThings only supports V2)
            0x98: 1, // Security S0 V1
            0x9F: 1  // Security S2 V1
           ]
}

/**
 *  getParametersMetadata()
 *
 *  Returns configuration parameters metadata. 
 *
 *  Reference: https://products.z-wavealliance.org/products/3399/configs
 **/
private getParametersMetadata()
{
    return [
        [id:  1, size: 1, type: "number", range: "0..15", defaultValue: "1", required: false, readonly: false,
		 name: "Operating pushbutton(s) for dimmer",
		 description: "This parameter specifies which pushbutton(s) that shall be used to control the built-in dimmer.\n" + 
		 "The parameter is a bitmask, so each of the values can be added together in order to have several pushbuttons to operate the dimmer.\n" +
		 "Values:\n0 = No local operation of the dimmer.\n1 = Pushbutton 1 controls the dimmer.\n2 = Pushbutton 2 controls the dimmer.\n3 = Pushbutton 3 controls the dimmer.\n" +
		 "4 = Pushbutton 4 controls the dimmer."],

		[id:  2, size: 1, type: "number", range: "0..255", defaultValue: "5", required: false, readonly: false,
		 name: "Duration of dimming",
		 description: "This parameter specifies the duration of a full regulation of the light from 0% to 100%.\n" +
		 "A regulation of the light with 1% will take 1/100 of the specified duration. This is used when a pushbutton \n" +
		 "is held-down for controlling the dimming, and when the dimming is fulfilled from other Z-Wave devices.\n" +
		 "Values:\n0 = Immediately\n1 - 127 = Duration in seconds\n128 - 255 = Duration in minutes (minus 127) from 1 – 128 minutes, where 128 is 1 minute"],

		[id:  3, size: 1, type: "number", range: "0..255", defaultValue: "0", required: false, readonly: false,
		 name: "Duration of on/off",
		 description: "This parameter specifies the duration when turning the light on or off.\n" +
		 "Values:\n0 = Immediately\n1 - 127 = Duration in seconds\n128 - 255 = Duration in minutes (minus 127) from 1 – 128 minutes, where 128 is 1 minute"],

		[id:  4, size: 1, type: "enum", defaultValue: "1", required: false, readonly: false,
		 name: "Dimmer mode",
		 description: "The dimmer can work in three different modes: on/off, leading edge or trailing edge.",
		 options: ["0" : "0: No dimming, only on/off (0/100%)",
                   "1" : "1: Trailing edge dimming",
                   "2" : "2: Leading edge dimming"]],
		 
		[id:  5, size: 1, type: "number", range: "0..99", defaultValue: "0", required: false, readonly: false,
		 name: "Dimmer minimum level",
		 description: "This parameter specifies the actual level of the dimmer output when set to 0%."],

		[id:  6, size: 1, type: "number", range: "0..99", defaultValue: "99", required: false, readonly: false,
		 name: "Dimmer maximum level",
		 description: "This parameter specifies the actual level of the dimmer output when set to 99%."],

		[id:  7, size: 1, type: "enum", defaultValue: "1", required: false, readonly: false,
		 name: "Central Scene notifications",
		 description: "This parameter can be used for disabling Central Scene notifications.",
		 options: ["0" : "0: Notifications are disabled",
                   "1" : "1: Notifications are enabled"]],

		[id:  8, size: 1, type: "enum", defaultValue: "1", required: false, readonly: false,
		 name: "Double-activation functionality",
		 description: "This parameter specifies the reaction when double-activating the pushbuttons.",
		 options: ["0" : "0: Double-activation disabled",
                   "1" : "1: Double-activation sets light to 100%"]],

		[id:  10, size: 1, type: "enum", defaultValue: "1", required: false, readonly: false,
		 name: "Enhanced LED control",
		 description: "This parameter can be used for enabling the enhanced LED control. See document about MATRIX enhanced LED control.",
		 options: ["0" : "0: Enhanced LED control is disabled",
                   "1" : "1: Enhanced LED control is enabled"]]

	]
}
