export default function App() {
    return (
        <div className="flex flex-col bg-gray-300 p-2 w-[1024px] h-[768px] mx-auto my-12 space-y-2 text-[#470000] font-['Playfair_Display']">
            <div className="bg-white">
                <div>
                    <div className='flex space-x-2'>
                        <div className='flex flex-col p-2 w-[12%] text-center'>
                            <div className='pb-2'>
                                <span>Terminal ID</span>
                            </div>
                            <div className='w-full bg-black h-[2px]'></div>
                            <div className='pt-2'>
                                <span>~~~~~~~</span>
                            </div>
                        </div>
                        <div className='flex flex-col p-2 w-[12%] text-center'>
                            <div className='pb-2'>
                                <span>Windows No.</span>
                            </div>
                            <div className='w-full bg-black h-[2px]'></div>
                            <div className='pt-2'>
                                <span>~~</span>
                            </div>
                        </div>
                    </div>
                    <div className="text-[84px] -mb-4 font-['Playfair_Display'] font-black px-2">
                        <span>Ticket Information</span>
                    </div>
                </div>
                <div></div>
            </div>
            <div className="flex space-x-1 w-full">
                <div className="flex space-x-1 w-full">
                    <div className="flex flex-col bg-white w-[20%] p-1">
                        <span className='text-lg'>From</span>
                        <span className="font-['Tiro_Devanagari_Hindi'] text-lg tracking-wide">कहाँ से</span>
                    </div>
                    <div className="flex flex-col bg-black text-white w-[80%] p-2 justify-center">
                        <span>~~~~~~~</span>
                    </div>
                </div>
                <div className='flex space-x-1 w-full'>
                    <div className="flex flex-col bg-white w-[20%] p-1">
                        <span className='text-lg'>To</span>
                        <span className="font-['Tiro_Devanagari_Hindi'] text-lg tracking-wide">कहाँ तक</span>
                    </div>
                    <div className="flex flex-col bg-black text-white w-[80%] p-2 justify-center">
                        <span>~~~~~~~</span>
                    </div>
                </div>
            </div>
            <div className="flex space-x-1 w-full">
                <div className='flex space-x-1 w-full'>
                    <div className='flex flex-col bg-white w-[20%] p-1'>
                        <span className='text-lg'>Date</span>
                        <span className="font-['Tiro_Devanagari_Hindi'] text-lg tracking-wide">दिनांक</span>
                    </div>
                    <div className='flex flex-col bg-black text-white w-[40%] p-1 justify-center'>
                        <span className='text-lg'>~~_~~_~~~~</span>
                    </div>
                    <div className='flex flex-col bg-white w-[20%] text-center p-1'>
                        <span className='text-lg'>Adult</span>
                        <span className='h-[2px] bg-black'></span>
                        <span className='text-lg'>Child</span>
                    </div>
                    <div className='flex flex-col bg-black text-white w-[20%] text-center p-1'>
                        <span className='text-lg'>~~</span>
                        <span className='h-[2px] bg-gray-100'></span>
                        <span className='text-lg'>~~</span>
                    </div>
                </div>
                <div className='flex space-x-1 w-full'>
                    <div className='flex flex-col bg-black text-white w-[19.5%] text-center p-1'>
                        <div className=''>
                            <span className='text-lg'>Class</span>
                        </div>
                        <span className='h-[2px] bg-gray-100'></span>
                        <div>
                            <span>~~</span>
                        </div>
                    </div>
                    <div className='flex flex-col bg-white w-[19.5%] p-1 px-2'>
                        <span className='text-lg'>Fare</span>
                        <span className="font-['Tiro_Devanagari_Hindi'] text-lg tracking-wide">किराया</span>
                    </div>
                    <div className='flex flex-col bg-[#470000] text-white w-[61%] text-center p-1 justify-center'>
                        <span className='text-3xl'>~~.~~</span>
                    </div>
                </div>
            </div>
            <div className="flex w-full space-x-1">
                <div className='flex flex-col space-y-1 w-[70%] py-20'>
                    <div className='flex space-x-1 w-full'>
                        <div className='flex space-x-1 w-full'>
                            <div className='flex flex-col bg-white w-[51%] p-2'>
                                <span className='text-xl'>Type of Train</span>
                                <span className="font-['Tiro_Devanagari_Hindi'] text-xl tracking-wide">ट्रेन का प्रकार</span>
                            </div>
                            <div className="flex flex-col bg-black text-white w-[50%] p-2 justify-center">
                                <span>~~~~~~~</span>
                            </div>
                        </div>
                        <div className='flex space-x-1 w-full'>
                            <div className='flex flex-col bg-white w-[30%] text-center p-1 py-2 justify-center'>
                                <span className='text-xl'>Pay Mode</span>
                            </div>
                            <div className="flex flex-col bg-black text-white w-[70%] p-2 justify-center">
                                <span>~~~~~~~</span>
                            </div>
                        </div>
                    </div>
                    <div className='flex w-full space-x-1'>
                        <div className='flex w-[50%] space-x-1'>
                            <div className='flex flex-col bg-white w-[51%] text-center p-2 py-5 justify-center'>
                                <span className='text-lg'>Transaction Type</span>
                            </div>
                            <div className="flex flex-col bg-black text-white w-[50%] p-2 justify-center">
                                <span>~~~~~~~</span>
                            </div>
                        </div>
                        <div className='bg-black w-[50%]'></div>
                    </div>
                </div>
                <div className='bg-white w-[30%]'></div>
            </div>
            <div className="flex space-x-1 w-full">
                <div className='flex space-x-1 w-[70%]'>
                    <div className='flex flex-col bg-white w-[25%] text-center p-2 justify-center'>
                        <span className='text-xl'>Operator</span>
                        <span className='text-xl'>Name</span>
                    </div>    
                    <div className='bg-black w-[75%]'></div>    
                </div>    
                <div className='flex flex-col bg-white text-[#470000] w-[30%] text-center p-1 justify-center'>
                    <span className="font-['Google_Sans_Flex'] font-normal text-2xl">North Eastern Railway</span>    
                    <span className="font-['Tiro_Devanagari_Hindi'] text-xl tracking-wide">वाराणसी मंडल</span>
                </div>    
            </div>      
        </div>
    );
}
