/*
 * Copyright (c) 2025 Matthew Nelson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
@file:Suppress("NOTHING_TO_INLINE", "VariableInitializerIsRedundant")

package io.matthewnelson.kmp.file.internal

import io.matthewnelson.kmp.file.File
import io.matthewnelson.kmp.file.errnoToIOException
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.E2BIG
import platform.posix.EACCES
import platform.posix.EADDRINUSE
import platform.posix.EADDRNOTAVAIL
import platform.posix.EAFNOSUPPORT
import platform.posix.EAGAIN
import platform.posix.EALREADY
import platform.posix.EBADF
import platform.posix.EBADMSG
import platform.posix.EBUSY
import platform.posix.ECANCELED
import platform.posix.ECHILD
import platform.posix.ECONNABORTED
import platform.posix.ECONNREFUSED
import platform.posix.EDEADLK
import platform.posix.EDESTADDRREQ
import platform.posix.EDOM
import platform.posix.EEXIST
import platform.posix.EFAULT
import platform.posix.EFBIG
import platform.posix.EHOSTUNREACH
import platform.posix.EIDRM
import platform.posix.EILSEQ
import platform.posix.EINPROGRESS
import platform.posix.EINTR
import platform.posix.EINVAL
import platform.posix.EIO
import platform.posix.EISCONN
import platform.posix.EISDIR
import platform.posix.ELOOP
import platform.posix.EMFILE
import platform.posix.EMLINK
import platform.posix.EMSGSIZE
import platform.posix.ENAMETOOLONG
import platform.posix.ENETDOWN
import platform.posix.ENETRESET
import platform.posix.ENETUNREACH
import platform.posix.ENFILE
import platform.posix.ENOBUFS
import platform.posix.ENODATA
import platform.posix.ENODEV
import platform.posix.ENOENT
import platform.posix.ENOEXEC
import platform.posix.ENOLCK
import platform.posix.ENOLINK
import platform.posix.ENOMEM
import platform.posix.ENOMSG
import platform.posix.ENOPROTOOPT
import platform.posix.ENOSPC
import platform.posix.ENOSR
import platform.posix.ENOSTR
import platform.posix.ENOSYS
import platform.posix.ENOTCONN
import platform.posix.ENOTDIR
import platform.posix.ENOTEMPTY
import platform.posix.ENOTSOCK
import platform.posix.ENOTSUP
import platform.posix.ENOTTY
import platform.posix.ENXIO
import platform.posix.EOPNOTSUPP
import platform.posix.EOVERFLOW
import platform.posix.EPERM
import platform.posix.EPIPE
import platform.posix.EPROTO
import platform.posix.EPROTONOSUPPORT
import platform.posix.EPROTOTYPE
import platform.posix.ERANGE
import platform.posix.EROFS
import platform.posix.ESPIPE
import platform.posix.ESRCH
import platform.posix.ETIME
import platform.posix.ETIMEDOUT
import platform.posix.ETXTBSY
import platform.posix.EXDEV
import platform.posix.errno
import platform.posix.strerror
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@ExperimentalForeignApi
internal fun errnoToString(errno: Int): String {
    var msg = when (errno) {
        E2BIG -> "E2BIG"
        EACCES -> "EACCES"
        EADDRINUSE -> "EADDRINUSE"
        EADDRNOTAVAIL -> "EADDRNOTAVAIL"
        EAFNOSUPPORT -> "EAFNOSUPPORT"
        EAGAIN -> "EAGAIN"
        EALREADY -> "EALREADY"
        EBADF -> "EBADF"
        EBADMSG -> "EBADMSG"
        EBUSY -> "EBUSY"
        ECANCELED -> "ECANCELED"
        ECHILD -> "ECHILD"
        ECONNABORTED -> "ECONNABORTED"
        ECONNREFUSED -> "ECONNREFUSED"
        ECONNREFUSED -> "ECONNREFUSED"
        EDEADLK -> "EDEADLK"
        EDESTADDRREQ -> "EDESTADDRREQ"
        EDOM -> "EDOM"
        EEXIST -> "EEXIST"
        EFAULT -> "EFAULT"
        EFBIG -> "EFBIG"
        EHOSTUNREACH -> "EHOSTUNREACH"
        EIDRM -> "EIDRM"
        EILSEQ -> "EILSEQ"
        EINPROGRESS -> "EINPROGRESS"
        EINTR -> "EINTR"
        EINVAL -> "EINVAL"
        EIO -> "EIO"
        EISCONN -> "EISCONN"
        EISDIR -> "EISDIR"
        ELOOP -> "ELOOP"
        EMFILE -> "EMFILE"
        EMLINK -> "EMLINK"
        EMSGSIZE -> "EMSGSIZE"
        ENAMETOOLONG -> "ENAMETOOLONG"
        ENETDOWN -> "ENETDOWN"
        ENETRESET -> "ENETRESET"
        ENETUNREACH -> "ENETUNREACH"
        ENFILE -> "ENFILE"
        ENOBUFS -> "ENOBUFS"
        ENODATA -> "ENODATA"
        ENODEV -> "ENODEV"
        ENOENT -> "ENOENT"
        ENOEXEC -> "ENOEXEC"
        ENOLCK -> "ENOLCK"
        ENOLINK -> "ENOLINK"
        ENOMEM -> "ENOMEM"
        ENOMSG -> "ENOMSG"
        ENOENT -> "ENOENT"
        ENOPROTOOPT -> "ENOPROTOOPT"
        ENOSPC -> "ENOSPC"
        ENOSR -> "ENOSR"
        ENOSTR -> "ENOSTR"
        ENOSYS -> "ENOSYS"
        ENOTCONN -> "ENOTCONN"
        ENOTDIR -> "ENOTDIR"
        ENOTEMPTY -> "ENOTEMPTY"
        ENOTSOCK -> "ENOTSOCK"
        ENOTSUP -> "ENOTSUP"
        ENOTTY -> "ENOTTY"
        ENXIO -> "ENXIO"
        EOPNOTSUPP -> "EOPNOTSUPP"
        EOVERFLOW -> "EOVERFLOW"
        EPERM -> "EPERM"
        EPIPE -> "EPIPE"
        EPROTO -> "EPROTO"
        EPROTONOSUPPORT -> "EPROTONOSUPPORT"
        EPROTOTYPE -> "EPROTOTYPE"
        ERANGE -> "ERANGE"
        EROFS -> "EROFS"
        ESPIPE -> "ESPIPE"
        ESRCH -> "ESRCH"
        ETIME -> "ETIME"
        ETIMEDOUT -> "ETIMEDOUT"
        ETXTBSY -> "ETXTBSY"
        EXDEV -> "EXDEV"
        else -> "errno[$errno]"
    }

    strerror(errno)?.toKString()?.ifBlank { null }?.let { msg += ": $it" }

    return msg
}

@ExperimentalForeignApi
internal inline fun errnoToIllegalArgumentOrIOException(errno: Int, file: File?, other: File? = null): Exception {
    return if (errno == EINVAL) {
        val message = errnoToString(errno)
        IllegalArgumentException(message)
    } else {
        errnoToIOException(errno, file, other)
    }
}

// action must be something that returns -1 and sets errno
@OptIn(ExperimentalContracts::class)
internal inline fun ignoreEINTR(action: () -> Int): Int {
    contract {
        callsInPlace(action, InvocationKind.UNKNOWN)
    }

    var ret = -1
    while (true) {
        ret = action()
        if (ret != -1) break
        if (errno == EINTR) continue
        break
    }
    return ret
}

// action must be something that returns CPointer<T>? and sets errno
@ExperimentalForeignApi
@OptIn(ExperimentalContracts::class)
internal inline fun <T: CPointed> ignoreEINTR(action: () -> CPointer<T>?): CPointer<T>? {
    contract {
        callsInPlace(action, InvocationKind.UNKNOWN)
    }

    var ret: CPointer<T>? = null
    while (true) {
        ret = action()
        if (ret != null) break
        if (errno == EINTR) continue
        break
    }
    return ret
}
