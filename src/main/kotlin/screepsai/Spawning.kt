package screepsai

import screeps.api.*
import screeps.api.structures.StructureSpawn
import screeps.utils.isEmpty
import screeps.utils.unsafe.delete
import screepsai.roles.*

val BODYPART_COST = hashMapOf(
    MOVE to 50,
    WORK to 100,
    ATTACK to 80,
    CARRY to 50,
    HEAL to 250,
    RANGED_ATTACK to 150,
    TOUGH to 10,
    CLAIM to 600
)

class Body(val parts: Array<BodyPartConstant>) {
    val cost = parts.sumOf { part -> BODYPART_COST[part]!! }
}

val BASE_BODY = Body(arrayOf(WORK, MOVE, CARRY))

val HARVESTER_BODIES = arrayOf(
    Body(arrayOf(WORK, WORK, MOVE)),
    Body(arrayOf(WORK, WORK, WORK, WORK, WORK, MOVE)),
)

val UPGRADER_BODIES = arrayOf(
    Body(arrayOf(WORK, MOVE, MOVE, CARRY, CARRY)),
    Body(arrayOf(MOVE, WORK, WORK, WORK, CARRY, CARRY, CARRY, CARRY))
)

val TRANSPORTER_BODIES = arrayOf(
    Body(arrayOf(MOVE, MOVE, CARRY, CARRY, CARRY, CARRY)),
    Body(arrayOf(MOVE, MOVE, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY))
)

val BUILDER_BODIES = arrayOf(
    Body(arrayOf(WORK, CARRY, CARRY, CARRY, MOVE)),
    Body(arrayOf(MOVE, MOVE, WORK, WORK, CARRY, CARRY, CARRY, CARRY, CARRY))
)

fun getBody(role: CreepRole, energyAvailable: Int): Body {
    val bodies = when (role) {
        CreepRole.UNASSIGNED  -> return BASE_BODY
        CreepRole.HARVESTER   -> HARVESTER_BODIES
        CreepRole.UPGRADER    -> UPGRADER_BODIES
        CreepRole.TRANSPORTER -> TRANSPORTER_BODIES
        CreepRole.BUILDER     -> BUILDER_BODIES
        CreepRole.MAINTAINER  -> BUILDER_BODIES
    }

    return bodies.last { it.cost <= energyAvailable }
}

fun spawnCreep(spawn: StructureSpawn, role: CreepRole, body: Body): ScreepsReturnCode {
    val newName = "creep_${role.name}_${Game.time}"
    val code = spawn.spawnCreep(body.parts, newName)
    when (code) {
        OK                    -> console.log("spawning $newName with body ${body.parts}")
        ERR_BUSY              -> console.log("Spawner ${spawn} in ${spawn.room} is busy")
        ERR_NOT_ENOUGH_ENERGY -> console.log("Not enough energy to spawn a new ${role.name}")
        else                  -> console.log("unhandled error code $code")
    }

    if (code != OK) {
        return code
    }

    val creep = Game.creeps[newName]!!
    creep.setRole(role)

    return code
}

fun spawnCreeps(
    role: CreepRole,
    room: Room
) {

    val body = try {
        getBody(role, room.energyAvailable)
    }
    catch (error: NoSuchElementException) {
        console.log("Couldn't determine body for ${role} with ${room.energyAvailable} energy")
        return
    }

    val spawns = room.find(FIND_MY_SPAWNS)

    for (spawn in spawns) {
        val code = spawnCreep(spawn, role, body)
        if (code == OK) {
            return
        }
    }
    console.log("Unable to spawn new ${role} creep in ${room}")
}

fun houseKeeping(creeps: Record<String, Creep>) {
    if (Game.creeps.isEmpty()) return  // this is needed because Memory.creeps is undefined

    for ((creepName, _) in Memory.creeps) {
        if (creeps[creepName] == null) {
            console.log("deleting obsolete memory entry for creep $creepName")
            delete(Memory.creeps[creepName])
        }
    }
}