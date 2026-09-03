package com.olegbelyanin.expensetracker.domain.category

class DuplicateCategoryNameException(name: String) : IllegalStateException("Category already exists: $name")

class EmptyCategoryNameException : IllegalArgumentException("Category name is empty after normalization")

class CategoryNotFoundException(id: Long) : NoSuchElementException("Category $id was not found")

class BuiltinCategoryLockedException(id: Long) : IllegalStateException("Builtin category $id cannot be edited")

class FallbackCategoryProtectedException : IllegalStateException("Fallback category cannot be archived")
