package com.zendesk.search.repo

/**
 * Used only for demonstrating that the datastructure representing
 * in-memory repo can be anything as far as there is a way to
 * implement `Repo`
 */
final case class InMemory[A](data: List[A])
