project(daemon)
cmake_minimum_required(VERSION 3.4.1)


set(CMAKE_STRIP ":")

add_subdirectory(${EXTERNAL_ROOT} external)

set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--version-script=${CMAKE_CURRENT_SOURCE_DIR}/version_script.txt")


set(SOURCES
        dex2oat.cpp
        logcat.cpp
        obfuscation.cpp
)

add_library(${PROJECT_NAME} SHARED ${SOURCES})


target_include_directories(${PROJECT_NAME} PUBLIC
        ${EXTERNAL_ROOT}/xDL/xdl/src/main/cpp/include)
target_include_directories(${PROJECT_NAME} PUBLIC
        ${EXTERNAL_ROOT}/lsplant/danlant/src/main/jni/include)

target_include_directories(${PROJECT_NAME} PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})




target_link_libraries(${PROJECT_NAME} PRIVATE
        danlant_static
        dex_builder_static
        xdl_static
        android
        log)

#if (DEFINED DEBUG_SYMBOLS_PATH)
#    message(STATUS "Debug symbols will be placed at ${DEBUG_SYMBOLS_PATH}")
#    add_custom_command(TARGET ${PROJECT_NAME} POST_BUILD
#            COMMAND ${CMAKE_COMMAND} -E make_directory ${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}
#            COMMAND ${CMAKE_OBJCOPY} --only-keep-debug $<TARGET_FILE:${PROJECT_NAME}>
#            ${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}/${PROJECT_NAME}.debug
#            COMMAND ${CMAKE_STRIP} --strip-all $<TARGET_FILE:${PROJECT_NAME}>
#            COMMAND ${CMAKE_OBJCOPY} --add-gnu-debuglink ${DEBUG_SYMBOLS_PATH}/${ANDROID_ABI}/${PROJECT_NAME}.debug
#            $<TARGET_FILE:${PROJECT_NAME}>)
#endif()