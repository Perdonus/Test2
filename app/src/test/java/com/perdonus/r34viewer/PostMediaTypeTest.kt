package com.perdonus.r34viewer

import com.perdonus.r34viewer.data.model.PostMediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class PostMediaTypeTest {
    @Test
    fun `detects video by file extension`() {
        assertEquals(PostMediaType.VIDEO, PostMediaType.fromUrl("https://cdn.test/file.webm"))
    }

    @Test
    fun `detects image by file extension`() {
        assertEquals(PostMediaType.IMAGE, PostMediaType.fromUrl("https://cdn.test/file.jpg"))
    }
}
