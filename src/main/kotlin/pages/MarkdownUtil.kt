package com.stableform.pages

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet

object MarkdownUtil {
    private val options = MutableDataSet().apply {
        // Enable GitHub Flavored Markdown features
        set(Parser.EXTENSIONS, listOf(
            com.vladsch.flexmark.ext.tables.TablesExtension.create(),
            com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension.create(),
            com.vladsch.flexmark.ext.autolink.AutolinkExtension.create(),
            com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension.create(),
            com.vladsch.flexmark.ext.footnotes.FootnoteExtension.create(),
            com.vladsch.flexmark.ext.typographic.TypographicExtension.create(),
            com.vladsch.flexmark.ext.definition.DefinitionExtension.create(),
            com.vladsch.flexmark.ext.emoji.EmojiExtension.create()
        ))
    }

    private val parser = Parser.builder(options).build()
    private val renderer = HtmlRenderer.builder(options).build()

    fun markdownToHtml(markdown: String): String {
        val document = parser.parse(markdown)
        return renderer.render(document)
    }
} 