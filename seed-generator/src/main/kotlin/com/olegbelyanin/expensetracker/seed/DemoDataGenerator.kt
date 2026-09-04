package com.olegbelyanin.expensetracker.seed

import com.olegbelyanin.expensetracker.domain.demo.DemoExpenseGenerator
import com.olegbelyanin.expensetracker.domain.demo.DemoExpenseTemplate
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

fun main(args: Array<String>) {
    DemoDataGenerator.write(DemoGeneratorArgs.parse(args))
}

object DemoDataGenerator {
    fun write(args: DemoGeneratorArgs): String {
        val catalog = CategoryCatalog.builtin()
        val train = DatasetReader.read(args.train, catalog)
        val templates = train.map { row -> DemoExpenseTemplate(row.name, row.location, row.categoryCode) }
        val goldens = SeedPipeline.goldenRows.map { row ->
            DemoExpenseTemplate(row.name, row.location, row.categoryCode)
        }
        val csv = DemoExpenseGenerator.writeCsv(
            templates = templates,
            count = args.count,
            idPrefix = args.prefix,
            goldens = goldens,
        )
        args.outputs.forEach { path ->
            path.parent?.let { Files.createDirectories(it) }
            path.writeText(csv)
            println("Wrote ${args.count} demo expenses to $path")
        }
        return csv
    }
}

data class DemoGeneratorArgs(val train: Path, val count: Int, val prefix: String, val outputs: List<Path>) {
    companion object {
        fun parse(args: Array<String>): DemoGeneratorArgs {
            val values = linkedMapOf<String, MutableList<String>>()
            var index = 0
            while (index < args.size) {
                val key = args[index]
                require(key.startsWith("--") && index + 1 < args.size) { "Usage: $USAGE" }
                values.getOrPut(key) { mutableListOf() }.add(args[index + 1])
                index += 2
            }
            val outputs = values["--output"].orEmpty().map(Path::of)
            require(outputs.isNotEmpty()) { "Missing --output. $USAGE" }
            return DemoGeneratorArgs(
                train = Path.of(values["--train"]?.singleOrNull() ?: error("Missing --train. $USAGE")),
                count = (values["--count"]?.singleOrNull() ?: DemoExpenseGenerator.UI_COUNT.toString()).toInt(),
                prefix = values["--prefix"]?.singleOrNull() ?: DemoExpenseGenerator.UI_PREFIX,
                outputs = outputs,
            )
        }

        private const val USAGE =
            "DemoDataGenerator --train FILE [--count N] [--prefix ui] --output FILE [--output FILE]"
    }
}
