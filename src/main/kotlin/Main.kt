import java.io.File
import java.nio.file.Files
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

const val STRETCH_MASK_VERTICAL_EACH = 2
fun main() {

    println("filler string: ")
    val filler = readln().trim()
    println("base string: ")
    val base = readln().trim()
    println("Zoom level")
    val zoom = readln().toInt()
    for (letter in base) {
        val mask = loadMask(letter.uppercase())
        println(fillMaskFromString(filler, extendMask(zoom, mask)))
    }
}

fun fillMaskFromString(fillerWord: String, mask: List<String>): String {
    var countUsedLetters = 0
    val resultStrings = mutableListOf<String>()
    for (row in mask) {
        val resultString = mutableListOf<String>()
        for (c in row) {
            if (c == '.') {
                resultString.add(" ")
            } else {
                resultString.add(fillerWord[countUsedLetters % fillerWord.length].toString())
                countUsedLetters++
            }
        }
        resultStrings.add(resultString.joinToString(""))
    }
    return resultStrings.joinToString("\n")
}


fun badVersion(fillerWord: String, mask: List<String>): String {
    return mask.joinToString("\n") { it.replace("*", fillerWord).replace(".", " ".repeat(fillerWord.length)) }
}

fun extendMask(zoom: Int, mask: List<String>): List<String> {
    val newMask = mutableListOf<String>()
    val extendedMask =
        mask.map { it.replace("*", "*".repeat(zoom)).replace(".", ".".repeat(zoom)) }
//    println(extendedMask.joinToString("\n"))
//    println("----")
    val mul = zoom / STRETCH_MASK_VERTICAL_EACH
    for (i in 0..extendedMask.size - 2) {
        val upperConnectivityComponents = getConnectivityComponents(extendedMask[i])
        val lowerConnectivityComponents = getConnectivityComponents(extendedMask[i + 1])
        if (upperConnectivityComponents.size == lowerConnectivityComponents.size) {
            println(1)
            val additionMasksLines =
                getAdditionStringsConnectingOneToOne(
                    upperConnectivityComponents,
                    lowerConnectivityComponents,
                    mul,
                    extendedMask[0].length
                )
            newMask.add(extendedMask[i])
            newMask.addAll(additionMasksLines)
        } else if (upperConnectivityComponents.size == 1) {
            println(2)
            val additionMasksLines = getAdditionStringsConnectingOneToMany(
                upperConnectivityComponents[0],
                lowerConnectivityComponents,
                mul,
                extendedMask[0].length
            )
            newMask.add(extendedMask[i])
            newMask.addAll(additionMasksLines)
        } else if (lowerConnectivityComponents.size == 1) {
            println(3)
            val additionMasksLines = getAdditionStringsConnectingOneToMany(
                lowerConnectivityComponents[0],
                upperConnectivityComponents,
                mul,
                extendedMask[0].length
            ).reversed()
            newMask.add(extendedMask[i])
            newMask.addAll(additionMasksLines)
        } else {
            val additionMasksLines = tryConnectFromBorders(
                upperConnectivityComponents,
                lowerConnectivityComponents,
                mul,
                extendedMask[0].length
            )
            newMask.addAll(additionMasksLines)
        }
    }
    newMask.add(extendedMask.last())
    return newMask
}
// TODO Этому методу вообще плохо
fun tryConnectFromBorders(
    upperConnectivityComponents: List<MutableList<Int>>,
    lowerConnectivityComponents: List<MutableList<Int>>,
    mul: Int,
    stringSize: Int
): List<String> {
    if (upperConnectivityComponents.size < lowerConnectivityComponents.size)
        return connectManyToMore(mul, stringSize, upperConnectivityComponents, lowerConnectivityComponents)
    return connectManyToMore(mul, stringSize, lowerConnectivityComponents, upperConnectivityComponents)
}

private fun connectManyToMore(
    mul: Int,
    stringSize: Int,
    many: List<MutableList<Int>>,
    more: List<MutableList<Int>>
): MutableList<String> {
    var i = 0
    val res = MutableList(mul) { ".".repeat(stringSize) }
    val relation = more.size.toFloat() / many.size
    while (i * 2 <= many.size) {
        val startManyComponent = many[i]
        val endManyComponent = many[many.size - 1 - i]
        val startMoreComponents = more.subList(i * relation.roundToInt(), min((i + 1) * relation.roundToInt(), more.lastIndex))
        val endMoreComponents =
            more.subList((more.size - (i + 1) * relation).roundToInt(), (more.size - (i) * relation).roundToInt())
        mergeMultiline(
            res,
            getAdditionStringsConnectingOneToMany(startManyComponent, startMoreComponents, mul, stringSize)
        )
        mergeMultiline(res, getAdditionStringsConnectingOneToMany(endManyComponent, endMoreComponents, mul, stringSize))
        i++
    }
    return res
}

fun getAdditionStringsConnectingOneToMany(
    upperConnectivityComponent: List<Int>,
    lowerConnectivityComponents: List<MutableList<Int>>,
    mul: Int,
    stringSize: Int,
    cutToLess: Boolean = true
): List<String> {
    val res = MutableList(mul) { ".".repeat(stringSize) }
    for ((cIndex, component) in lowerConnectivityComponents.withIndex()) {
        if (areNotClose(upperConnectivityComponent, component)) continue
        val passComponent = if (!cutToLess) {
            listOf(upperConnectivityComponent)
        } else if (0 == cIndex) {//первый интерполируем в начало
            listOf(upperConnectivityComponent.subList(0, component.size))
        } else if (lowerConnectivityComponents.size - 1 == cIndex) { // последний интерполируем в конец
            listOf(
                upperConnectivityComponent.subList(
                    max(0, upperConnectivityComponent.size - component.size),
                    upperConnectivityComponent.size
                )
            )
        } else { // остальные сами в себя
            listOf(
                component
            )
        }
        val additionOneToOne = getAdditionStringsConnectingOneToOne(passComponent, listOf(component), mul, stringSize)
        mergeMultiline(res, additionOneToOne)
    }
    return res
}

private fun mergeMultiline(
    res: MutableList<String>,
    additionOneToOne: List<String>
) {
    for (i in res.indices) {
        val t = merge(res[i], additionOneToOne[i])
        res[i] = t
    }
}

private fun merge(
    first: String, second: String
): String {
    val t = mutableListOf<Char>()
    for (j in first.indices) {
        if (first[j] == '*' || second[j] == '*') {
            t.add('*')
        } else {
            t.add('.')
        }
    }
    return t.joinToString("")
}

fun areNotClose(
    upperConnectivityComponent: List<Int>,
    component: MutableList<Int>,
    limit: Int = component.size * 2
): Boolean {
    return abs(upperConnectivityComponent.first() - component.first()) >= limit
            && abs(upperConnectivityComponent.last() - component.last()) >= limit
            && !(
            upperConnectivityComponent.all { component.contains(it) }
                    || component.all { upperConnectivityComponent.contains(it) }
            )
}

fun getAdditionStringsConnectingOneToOne(
    upperConnectivityComponents: List<List<Int>>,
    lowerConnectivityComponents: List<List<Int>>,
    mul: Int,
    stringSize: Int
): List<String> {//TODO добавить checkClose
    val res = mutableListOf<String>()
    for (i in 1..mul) {
        val moveOnPercent = i.toFloat() / (mul + 1)
        val components = mutableListOf<List<Int>>()
        for ((upper, lower) in upperConnectivityComponents.zip(lowerConnectivityComponents)) {
            if (maxOf(upper.size / lower.size, lower.size / upper.size) < 3) {
                val startMoved = lower.first() - upper.first()
                val endMoved = lower.last() - upper.last()
                val startOnThisInterpolationStep = startMoved * moveOnPercent + upper.first()
                val endOnThisInterpolationStep = endMoved * moveOnPercent + upper.last()
                val newConnectivityComponent =
                    (startOnThisInterpolationStep.roundToInt()..endOnThisInterpolationStep.roundToInt()).toList()
                components.add(newConnectivityComponent)
            } else {
                components.add(listOf(upper, lower).minBy { it.size })
            }
        }
        val mask = connectivityComponentsToMask(components, stringSize)
        res.add(mask)
    }
    return res
}

fun connectivityComponentsToMask(components: MutableList<List<Int>>, stringSize: Int): String {
    val stringAsArray = Array(stringSize) { '.' }
    for (index in components.flatten()) {
        stringAsArray[index] = '*'
    }
    return stringAsArray.joinToString("")
}

private fun getConnectivityComponents(
    sourceString: String
): List<MutableList<Int>> {
    val res = mutableListOf<MutableList<Int>>()
    var currentAsteriskChain = mutableListOf<Int>()
    for (j in sourceString.indices) {
        if (sourceString[j] == '.') {
            if (currentAsteriskChain.isNotEmpty()) {
                res.add(currentAsteriskChain)
                currentAsteriskChain = mutableListOf()
            }
        } else {
            currentAsteriskChain.add(j)
        }
    }
    if (currentAsteriskChain.isNotEmpty()) {
        res.add(currentAsteriskChain)
    }
    return res.toList()
}

fun loadMask(symbol: String): MutableList<String> {
    return Files.readAllLines(File("src/masks/${symbol}").toPath())!!
}