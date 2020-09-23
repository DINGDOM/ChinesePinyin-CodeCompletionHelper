package com.github.tuchg.nonasciicodecompletionhelper.extensions

import com.github.tuchg.nonasciicodecompletionhelper.model.ChineseLookupElement
import com.github.tuchg.nonasciicodecompletionhelper.utils.countContainsSomeChar
import com.github.tuchg.nonasciicodecompletionhelper.utils.toPinyin
import com.intellij.codeInsight.completion.*
import pansong291.simplepinyin.Pinyin

/**
 * @author tuchg
 * @date 2020-8-1
 */
class ChineseCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val resultSet = result
                .withPrefixMatcher(ChinesePrefixMatcher(result.prefixMatcher.prefix.toLowerCase()))
                .withRelevanceSorter(CompletionSorter.emptySorter())

        resultSet.addLookupAdvertisement("输入拼音,补全中文标识符;若无满意结果,请再次激活补全快捷键或给出更精确的输入")
        /**
         * todo 可暴力解决 bug:[需二次激活获取补全] 但性能影响较大
         * parameters.withInvocationCount(2)
         */
        // 先跳过当前 Contributors 获取包装后的 lookupElement而后进行修改装饰
        resultSet.runRemainingContributors(parameters, { r ->
            val element = r.lookupElement
            if (Pinyin.hasChinese(element.lookupString)) {
                var flag = 0
                // 从多音字列表提取命中次数最多的一个
                val closest = toPinyin(element.lookupString, Pinyin.FIRST_UP_CASE).let {
                    val prefix = r.prefixMatcher.prefix
                    if (it.size == 1) {
                        return@let if (countContainsSomeChar(it[0].toLowerCase(), prefix) >= prefix.length) {
                            flag += 100
                            it[0]
                        } else null
                    }
                    it.maxByOrNull { str ->
                        val count = countContainsSomeChar(str.toLowerCase(), prefix)
                        if (count >= prefix.length) {
                            flag++
                            count
                        } else -1
                    }
                }
                closest?.let {
                    if (flag > 0) {
                        // 追加补全列表
                        resultSet.addElement(ChineseLookupElement(flag, element.lookupString, it).copyFrom(r.lookupElement))
                    }
                }
            } else
                resultSet.passResult(r)
        }, true)
        resultSet.restartCompletionOnAnyPrefixChange()
    }
}

class ChinesePrefixMatcher(prefix: String) : PlainPrefixMatcher(prefix) {

    override fun prefixMatches(name: String): Boolean {
        return if (Pinyin.hasChinese(name)) {
            for (s in toPinyin(name, Pinyin.LOW_CASE)) {
                if (countContainsSomeChar(s, prefix) >= prefix.length) {
                    return true
                }
            }
            return false
        } else super.prefixMatches(name)
    }

    override fun cloneWithPrefix(prefix: String) = if (prefix == this.prefix) this else ChinesePrefixMatcher(prefix)
}
