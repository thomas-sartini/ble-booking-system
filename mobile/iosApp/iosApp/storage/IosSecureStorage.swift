import Foundation
import Security
import SharedLogic

final class IosSecureStorage: NativeStorageDelegate, Sendable {
    static let shared = IosSecureStorage()

    private let service: String
    private let lock = NSLock()

    init(
        service: String =
            Bundle.main.bundleIdentifier ?? "com.app.default"
    ) {
        self.service = service
    }

    func save(
        key: String,
        value_ value: String
    ) -> NativeStorageResult {
        perform {
            let query = self.baseQuery(for: key)
            let data = Data(value.utf8)

            let attributes: [String: Any] = [
                kSecValueData as String: data
            ]

            let updateStatus = SecItemUpdate(
                query as CFDictionary,
                attributes as CFDictionary
            )

            if updateStatus == errSecItemNotFound {
                var item = query
                item[kSecValueData as String] = data
                item[kSecAttrAccessible as String] =
                    kSecAttrAccessibleWhenUnlockedThisDeviceOnly

                let addStatus = SecItemAdd(
                    item as CFDictionary,
                    nil
                )

                guard addStatus == errSecSuccess else {
                    throw self.keychainError(
                        addStatus,
                        operation: "save"
                    )
                }
            } else if updateStatus != errSecSuccess {
                throw self.keychainError(
                    updateStatus,
                    operation: "update"
                )
            }

            return nil
        }
    }

    func get(key_ key: String) -> NativeStorageResult {
        perform {
            var query = self.baseQuery(for: key)
            query[kSecReturnData as String] = true
            query[kSecMatchLimit as String] = kSecMatchLimitOne

            var result: CFTypeRef?
            let status = SecItemCopyMatching(
                query as CFDictionary,
                &result
            )

            if status == errSecItemNotFound {
                return nil
            }

            guard status == errSecSuccess else {
                throw self.keychainError(
                    status,
                    operation: "read"
                )
            }

            guard let data = result as? Data,
                  let value = String(data: data, encoding: .utf8) else {
                throw self.keychainError(
                    errSecDecode,
                    operation: "decode"
                )
            }

            return value
        }
    }

    func remove(key_ key: String) -> NativeStorageResult {
        perform {
            let status = SecItemDelete(
                self.baseQuery(for: key) as CFDictionary
            )

            guard status == errSecSuccess ||
                  status == errSecItemNotFound else {
                throw self.keychainError(
                    status,
                    operation: "delete"
                )
            }

            return nil
        }
    }

    private func perform(
        _ operation: () throws -> String?
    ) -> NativeStorageResult {
        lock.lock()
        defer { lock.unlock() }

        do {
            let value = try operation()
            return NativeStorageResult(
                value: value,
                errorMessage: nil
            )
        } catch {
            return NativeStorageResult(
                value: nil,
                errorMessage: error.localizedDescription
            )
        }
    }

    private func baseQuery(for key: String) -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key
        ]
    }

    private func keychainError(
        _ status: OSStatus,
        operation: String
    ) -> NSError {
        let description = SecCopyErrorMessageString(status, nil)
            .map { $0 as String } ?? "Unknown Keychain error"

        return NSError(
            domain: "IosSecureStorage",
            code: Int(status),
            userInfo: [
                NSLocalizedDescriptionKey:
                    "Keychain \(operation) failed: \(description)"
            ]
        )
    }
}
