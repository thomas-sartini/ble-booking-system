import Foundation
import XCTest
import SharedLogic
@testable import Mobile

final class IosSecureStorageTests: XCTestCase {
    private var storage: NativeSecureStorageAdapter!
    private var prefix: String!
    private var keys: [String] = []

    override func setUp() {
        super.setUp()

        storage = NativeSecureStorageAdapter(
            delegate: IosSecureStorage()
        )
        prefix = "storage-test-\(UUID().uuidString)"
        keys = []
    }

    override func tearDownWithError() throws {
        defer {
            keys = []
            storage = nil
            prefix = nil
        }

        for key in keys {
            do {
                try storage.remove(key: key)
            } catch {
                XCTFail("Cleanup failed for \(key): \(error)")
            }
        }

        try super.tearDownWithError()
    }

    private func key(_ name: String) -> String {
        let value = "\(prefix!)-\(name)"
        keys.append(value)
        return value
    }

    func testSavesAndReadsUnicodeValue() throws {
        let key = key("unicode")

        try storage.save(key: key, value: "token-is-🔑")

        let value = try storage.get(key: key)
        XCTAssertEqual(value, "token-is-🔑")
    }

    func testOverwritesExistingValue() throws {
        let key = key("overwrite")

        try storage.save(key: key, value: "old-token")
        try storage.save(key: key, value: "new-token")

        let value = try storage.get(key: key)
        XCTAssertEqual(value, "new-token")
    }

    func testRemovesValueAndAllowsRepeatedRemoval() throws {
        let key = key("remove")

        try storage.save(key: key, value: "token")
        try storage.remove(key: key)
        try storage.remove(key: key)

        let value = try storage.get(key: key)
        XCTAssertNil(value)
    }

    func testReturnsNullForMissingKey() throws {
        let value = try storage.get(key: key("missing"))

        XCTAssertNil(value)
    }

    func testReadsValueFromAnotherInstance() throws {
        let key = key("persistent")

        try storage.save(key: key, value: "token")

        let anotherStorage = NativeSecureStorageAdapter(
            delegate: IosSecureStorage()
        )

        let value = try anotherStorage.get(key: key)
        XCTAssertEqual(value, "token")
    }
}
