package com.jainhardik120.automation.data

import kotlin.math.min

enum class MouseActions(val code: Int) {
    MOUSE_MOVE(145),
    MOUSE_SCROLL_UP(146),
    MOUSE_SCROLL_DOWN(147),
    MOUSE_BUTTON_LEFT(153),
    MOUSE_BUTTON_RIGHT(154),
    MOUSE_BUTTON_MIDDLE(156)
}

sealed class KeyAction {
    data class WriteString(val stringNum: Int) : KeyAction()
    data class MacroAction(val noOfSteps: Int, val actions: List<KeyAction>) : KeyAction()
    data class KeyCombo(val noOfKeys: Int, val keys: List<Int>) : KeyAction()
    data class SimSingleKey(val key: Int) : KeyAction()
    data class SingleKeyPress(val key: Int) : KeyAction()
    data class Delay(val multiplier: Int, val duration: Int) : KeyAction()
    data class MouseMove(val x: Int, val y: Int) : KeyAction()
    data class MouseAction(val action: MouseActions) : KeyAction()
    data object SendPacketToController : KeyAction()
    data class SendImmediateString(val string: String) : KeyAction()
    data object Null : KeyAction()
}

const val NUM_KEYS_KEYPAD = 40
const val NUM_STRINGS_KEYPAD = 64

fun createProfilePacket(strings: List<String>, actions: List<KeyAction>): ByteArray {
    val data: MutableList<Byte> = mutableListOf()
    data.add(0x80.toByte())
    data.add(0x00.toByte())
    data.add(0x00.toByte())
    if (actions.size != NUM_KEYS_KEYPAD) {
        throw Exception("Actions size need to be $NUM_KEYS_KEYPAD")
    }
    if (strings.size > NUM_STRINGS_KEYPAD) {
        throw Exception("Strings must be less than equal to $NUM_STRINGS_KEYPAD")
    }
    for (action in actions) {
        data.addAll(action.toByteInstruction().toList())
    }
    data.add(0xfe.toByte())
    for (string in strings) {
        if (string.length > 255) {
            throw Exception("String can be maximum 255 characters long")
        }
        data.add(string.length.toByte())
        data.addAll(string.toByteArray().toList())
    }
    data.add(0xff.toByte())
    val len = data.size - 3
    data[1] = (len shr 8 and 0xFF).toByte()
    data[2] = (len and 0xFF).toByte()
    return data.toByteArray()
}

fun KeyAction.toCreateInstructionPacket(state : Boolean) : ByteArray{
    val data: MutableList<Byte> = mutableListOf()
    data.add((0x88 or (if (state) 0x01 else 0x00)).toByte())
    data.add(0x00.toByte())
    data.add(0x00.toByte())
    data.addAll(this.toByteInstruction().toList())
    val len = data.size - 3
    data[1] = (len shr 8 and 0xFF).toByte()
    data[2] = (len and 0xFF).toByte()
    return data.toByteArray()
}

fun KeyAction.toByteInstruction(): ByteArray {
    val data: MutableList<Byte> = mutableListOf()
    when (this) {
        is KeyAction.Delay -> {
            data.add(0x8a.toByte())
            data.add(((min(multiplier, 15) shl 4) or min(duration, 15)).toByte())
        }

        is KeyAction.KeyCombo -> {
            data.add(((8 shl 4) or (7 and noOfKeys)).toByte())
            for (i in keys) {
                data.add(min(i, 255).toByte())
            }
        }

        is KeyAction.MacroAction -> {
            data.add(((4 shl 4) or (63 and noOfSteps)).toByte())
            for (i in actions) {
                data.addAll(i.toByteInstruction().toList())
            }
        }

        is KeyAction.MouseAction -> {
            data.add((action.code).toByte())
        }

        is KeyAction.MouseMove -> {
            data.add(MouseActions.MOUSE_MOVE.code.toByte())
            data.add(x.toByte())
            data.add(y.toByte())
        }

        is KeyAction.SendImmediateString -> {
            if (string.length > 255) {
                data.add(0xa0.toByte())
            } else {
                data.add(0xb0.toByte())
                data.add(min(string.length, 255).toByte())
                for (i in string) {
                    data.add(i.code.toByte())
                }
            }
        }

        KeyAction.SendPacketToController -> {
            data.add(0xa0.toByte())
        }

        is KeyAction.SimSingleKey -> {
            data.add(0x88.toByte())
            data.add(key.toByte())
        }

        is KeyAction.SingleKeyPress -> {
            data.add(0x89.toByte())
            data.add(key.toByte())
        }

        is KeyAction.WriteString -> {
            data.add(min(63, stringNum).toByte())
        }

        is KeyAction.Null -> {
            data.add(0xb1.toByte())
        }
    }
    return data.toByteArray()
}