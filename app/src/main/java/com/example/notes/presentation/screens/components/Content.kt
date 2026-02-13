package com.example.notes.presentation.screens.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.notes.domain.ContentItem


@Composable
fun Content(
    modifier: Modifier = Modifier,
    content: List<ContentItem>,
    onDeleteImageClick: (Int) -> Unit,
    onTextChanged: (Int, String) -> Unit
) {
    LazyColumn(
        modifier = modifier
    ) {
        content.forEachIndexed { index, contentItem ->
            item(key = index) {
                when (contentItem) {

                    is ContentItem.Image -> {
                        val isAlreadyDisplayed =
                            index > 0 && content[index - 1] is ContentItem.Image

                        content.takeIf { !isAlreadyDisplayed }
                            ?.drop(index)
                            ?.takeWhile { it is ContentItem.Image }
                            ?.map { (it as ContentItem.Image).url }
                            ?.let { urls ->
                                ImageGroup(
                                    modifier = Modifier.padding(horizontal = 24.dp),
                                    imageUrls = urls,
                                    onDeleteImageClick = { imageIndex ->
                                        onDeleteImageClick(imageIndex + index)
                                    }
                                )
                            }
                    }

                    is ContentItem.Text -> {
                        TextContent(
                            text = contentItem.content,
                            onTextChanged = {
                                onTextChanged(index, it)
                            }
                        )
                    }
                }
            }
        }
    }
}