# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [0.1.0] 2024-09-29

- Add Slack Notifier
- Support Whitelisting and Blacklisting for UserTargeting in Metadata

## [0.0.9]
### Added
- Support FeatureFlagResult method

## [0.0.7]
### Added

- Refactors the feature flag implementation to enhance type safety, flexibility, and correct behavior:
  * Replace Map with FeatureFlag objects for stronger typing
  * Introduce MetadataContent sealed class for structured and type-safe metadata
  * Update FeatureFlagService, Repository, and Cache to work with new FeatureFlag and MetadataContent structure
  * Improve the logic in isFeatureFlagEnabled function for correct behavior
  * Revise all tests to use FeatureFlag objects and validate new metadata handling

## [0.0.5]
### Added

- Initial release
